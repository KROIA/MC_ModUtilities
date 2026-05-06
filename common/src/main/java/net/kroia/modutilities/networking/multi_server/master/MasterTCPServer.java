package net.kroia.modutilities.networking.multi_server.master;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.multi_server.MultiServerPacketRegistry;
import net.kroia.modutilities.networking.multi_server.codec.PayloadDecoder;
import net.kroia.modutilities.networking.multi_server.codec.PayloadEncoder;
import net.kroia.modutilities.networking.multi_server.payload.ForwardPacketPayload;
import net.kroia.modutilities.networking.multi_server.payload.ManualDisconnectionPayload;
import net.kroia.modutilities.networking.multi_server.payload.Payload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The TCP listener side of the multi-server architecture.
 * <p>
 * Slave servers connect to this server, identify themselves via a handshake,
 * and exchange {@link Payload} objects over a length-prefixed Netty pipeline.
 * Routing helpers are provided to send packets to a single slave or broadcast
 * to all (or a subset of) connected slaves.
 *
 * @apiNote
 * Lifecycle is controlled via {@link #start()} and {@link #stop()}. The Netty
 * pipeline runs on its own I/O threads, so calls into this class from the
 * Minecraft server thread are safe.
 */
public class MasterTCPServer {


    /** All currently connected child servers, keyed by their serverId. */
    private final Map<String, Channel> CHILD_SERVERS = new ConcurrentHashMap<>();

    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private Channel serverChannel;
    private final String sharedSecret;
    private final MinecraftServer mcServer;
    private final int tcpPort;
    private String serverIP = "";

    private @Nullable Throwable startupFailReason = null;

    private final Runnable onServerStartSuccess;
    private final Consumer<Throwable> onServerStartFailure;
    private final Consumer<String> onSlaveConnected;
    private final Consumer<String> onSlaveDisconnected;


    /**
     * Constructs a new master TCP server.
     *
     * @param mcServer             The Minecraft server instance this master is bound to (used for registry access).
     * @param sharedSecret         The shared secret token that connecting slaves must present during the handshake.
     * @param tcpPort              The TCP port this server will bind to.
     * @param onServerStartSuccess Callback invoked when the listener has bound the port successfully.
     * @param onServerStartFailure Callback invoked with the failure cause if the listener could not bind.
     * @param onSlaveConnected     Callback invoked with the slave ID when a slave completes its handshake.
     * @param onSlaveDisconnected  Callback invoked with the slave ID when a previously-connected slave disconnects.
     */
    public MasterTCPServer(MinecraftServer mcServer, String sharedSecret, int tcpPort,
                           Runnable onServerStartSuccess, Consumer<Throwable> onServerStartFailure,
                           Consumer<String> onSlaveConnected, Consumer<String> onSlaveDisconnected)
    {
        this.mcServer = mcServer;
        this.sharedSecret = sharedSecret;
        this.tcpPort = tcpPort;
        this.onServerStartSuccess = onServerStartSuccess;
        this.onServerStartFailure = onServerStartFailure;
        this.onSlaveConnected = onSlaveConnected;
        this.onSlaveDisconnected = onSlaveDisconnected;
    }

    // ── Start / Stop ─────────────────────────────────────────────────────────

    /**
     * Starts the TCP listener on the configured port.
     * <p>
     * Once the bind completes, either {@code onServerStartSuccess} or
     * {@code onServerStartFailure} (provided to the constructor) is invoked.
     * On success, the local IP address is resolved via
     * {@link java.net.InetAddress#getLocalHost()} and exposed via {@link #getMasterIP()}.
     *
     * @apiNote
     * Safe to call from the Minecraft server thread — Netty runs on its own
     * I/O threads. The call returns immediately; the bind itself happens
     * asynchronously.
     */
    public void start()
    {
        info("Starting TCP Server on port " + tcpPort);
        bossGroup  = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        MasterTCPServer tcpServer = this;
        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                // ① Split the TCP stream into frames using a 3-byte length prefix
                                .addLast(new LengthFieldBasedFrameDecoder(1 << 21, 0, 3, 0, 3))
                                // ② Prepend a 3-byte length to every outgoing frame
                                .addLast(new LengthFieldPrepender(3))
                                // ③ bytes → Payload
                                .addLast(new PayloadDecoder())
                                // ④ Payload → bytes
                                .addLast(new PayloadEncoder())
                                // ⑤ Your routing logic
                                .addLast(new MasterPacketHandler(mcServer, tcpServer));
                    }
                });

        bootstrap.bind(tcpPort).addListener((ChannelFuture future) -> {
            if (future.isSuccess()) {
                serverChannel = future.channel();
                startupFailReason = null;

                // Get the local IP without making any external network connections.
                // The previous approach of opening a socket to 8.8.8.8 hangs on firewalled networks.
                try {
                    serverIP = java.net.InetAddress.getLocalHost().getHostAddress();
                } catch (java.net.UnknownHostException e) {
                    serverIP = "127.0.0.1";
                }

                info("TCP listener started on port "+ tcpPort);
                onServerStartSuccess.run();
            } else {
                startupFailReason = future.cause();
                serverIP = "";
                error("Failed to bind TCP port: "+ tcpPort, startupFailReason);
                onServerStartFailure.accept(startupFailReason);
            }
        });
    }

    /**
     * Stops the TCP listener and shuts down the underlying Netty event loop groups.
     * <p>
     * Closes the server channel and gracefully terminates the boss and worker
     * groups. After calling this method, any further routing calls will be no-ops
     * (or fail) until {@link #start()} is invoked again.
     *
     * @apiNote
     * Safe to call even if {@link #start()} was never invoked or failed.
     */
    public void stop() {
        if (serverChannel != null) serverChannel.close();
        if (bossGroup  != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        startupFailReason =  null;
        info("TCP listener stopped.");
    }

    // ── Routing helpers ───────────────────────────────────────────────────────

    /**
     * Wraps the given Minecraft custom packet in a {@link ForwardPacketPayload}
     * and sends it to the slave identified by {@code targetServerID}.
     *
     * @param senderPlayerUUID The UUID of the player originating the packet, or {@code null}
     *                         if the packet is not associated with a specific player.
     * @param targetServerID   The slave ID to send the packet to.
     * @param packet           The Minecraft custom packet payload to forward.
     * @return {@code true} if the slave was connected and the packet was scheduled for write,
     *         {@code false} if no active channel exists for the given slave ID.
     */
    public boolean sendToSlave(@Nullable UUID senderPlayerUUID, String targetServerID, CustomPacketPayload packet) {
        ForwardPacketPayload payload = MultiServerPacketRegistry.createForwardPacketPayload(senderPlayerUUID,"", packet);
        return sendToSlave(targetServerID, payload);
    }

    /**
     * Wraps the given Minecraft custom packet in a {@link ForwardPacketPayload}
     * and broadcasts it to every currently-connected slave server.
     *
     * @param senderPlayerUUID The UUID of the player originating the packet, or {@code null}
     *                         if the packet is not associated with a specific player.
     * @param packet           The Minecraft custom packet payload to forward.
     */
    public void broadcastToSlaves(@Nullable UUID senderPlayerUUID, CustomPacketPayload packet) {
        ForwardPacketPayload payload = MultiServerPacketRegistry.createForwardPacketPayload(senderPlayerUUID, "", packet);
        broadcastToSlaves(payload);
    }

    /**
     * Broadcasts the given Minecraft custom packet to every connected slave
     * EXCEPT the one identified by {@code excludeServerId}.
     *
     * @param senderPlayerUUID The UUID of the player originating the packet, or {@code null}
     *                         if the packet is not associated with a specific player.
     * @param packet           The Minecraft custom packet payload to forward.
     * @param excludeServerId  The slave ID that should NOT receive this broadcast.
     */
    public void broadcastToSlaves(@Nullable UUID senderPlayerUUID, CustomPacketPayload packet, String excludeServerId) {
        ForwardPacketPayload payload = MultiServerPacketRegistry.createForwardPacketPayload(senderPlayerUUID, "", packet);
        broadcastToSlaves(payload, excludeServerId);
    }

    /**
     * Broadcasts the given Minecraft custom packet to every connected slave
     * EXCEPT those whose slave IDs are contained in {@code excludeServerIds}.
     *
     * @param senderPlayerUUID The UUID of the player originating the packet, or {@code null}
     *                         if the packet is not associated with a specific player.
     * @param packet           The Minecraft custom packet payload to forward.
     * @param excludeServerIds The slave IDs that should NOT receive this broadcast.
     */
    public void broadcastToSlaves(@Nullable UUID senderPlayerUUID, CustomPacketPayload packet, List<String> excludeServerIds) {
        ForwardPacketPayload payload = MultiServerPacketRegistry.createForwardPacketPayload(senderPlayerUUID, "", packet);
        broadcastToSlaves(payload, excludeServerIds);
    }



    /**
     * Sends a raw {@link Payload} to a single connected slave.
     *
     * @param targetServerID The slave ID to send the payload to.
     * @param payload        The payload to send.
     * @return {@code true} if the slave was connected and the payload was scheduled for write,
     *         {@code false} if no active channel exists for the given slave ID.
     */
    public boolean sendToSlave(String targetServerID, Payload payload) {
        Channel ch = CHILD_SERVERS.get(targetServerID);
        if (ch != null && ch.isActive()) {
            ch.writeAndFlush(payload);
            return true;
        } else {
            warn("sendToChild: server '"+targetServerID+"' not connected");
            return false;
        }
    }

    /**
     * Broadcasts a raw {@link Payload} to every currently-connected slave server.
     *
     * @param payload The payload to broadcast.
     */
    public void broadcastToSlaves(Payload payload) {
        CHILD_SERVERS.forEach((id, ch) -> {
            ch.writeAndFlush(payload);
        });
    }

    /**
     * Broadcasts a raw {@link Payload} to every connected slave EXCEPT the one
     * identified by {@code excludeServerId}.
     *
     * @param payload         The payload to broadcast.
     * @param excludeServerId The slave ID that should NOT receive this broadcast.
     */
    public void broadcastToSlaves(Payload payload, String excludeServerId) {
        CHILD_SERVERS.forEach((id, ch) -> {
            if (!id.equals(excludeServerId) && ch.isActive()) {
                ch.writeAndFlush(payload);
            }
        });
    }

    /**
     * Broadcasts a raw {@link Payload} to every connected slave EXCEPT those
     * whose slave IDs are contained in {@code excludeServerIds}.
     *
     * @param payload          The payload to broadcast.
     * @param excludeServerIds The slave IDs that should NOT receive this broadcast.
     */
    public void broadcastToSlaves(Payload payload, List<String> excludeServerIds) {
        CHILD_SERVERS.forEach((id, ch) -> {
            if(!excludeServerIds.contains(id)) {
                ch.writeAndFlush(payload);
            }
        });
    }


    String getSharedSecret() {
        return sharedSecret;
    }

    void putChildConnection(String serverId, Channel channel) {
        CHILD_SERVERS.put(serverId, channel);
        info("Child server '"+serverId+"' connected to this server.");
        onSlaveConnected.accept(serverId);
    }
    void removeChildConnection(String serverId) {
        if(CHILD_SERVERS.remove(serverId) != null) {
            info("Child server '" + serverId + "' disconnected.");
            onSlaveDisconnected.accept(serverId);
        }
    }


    /**
     * @return {@code true} if the TCP listener is bound and active, {@code false} otherwise.
     */
    public boolean isRunning() {
        return serverChannel != null && serverChannel.isActive();
    }

    /**
     * @return {@code true} if the most recent {@link #start()} attempt failed
     *         to bind the configured port.
     */
    public boolean isStartupFailed() {
        return startupFailReason != null;
    }

    /**
     * @return The {@link Throwable} that caused the most recent startup failure,
     *         or {@code null} if startup succeeded or has not been attempted.
     */
    public @Nullable Throwable getStartupFailReason() {
        return startupFailReason;
    }

    /**
     * @return The TCP port this server is configured to listen on.
     */
    public int getPort()
    {
        return tcpPort;
    }

    /**
     * @return The local IP address the master server is reachable at, as resolved
     *         by {@link java.net.InetAddress#getLocalHost()} during {@link #start()}.
     *         Empty string if the server has not started or startup failed.
     */
    public String getMasterIP()
    {
        return serverIP;
    }

    /**
     * @return A snapshot list of the slave IDs currently connected to this master.
     */
    public List<String> getConnectedSlaveIDs()
    {
        List<String> slaves = new ArrayList<>();
        CHILD_SERVERS.forEach((id, ch) -> {
            slaves.add(id);
        });
        return slaves;
    }

    /**
     * Sends a {@link ManualDisconnectionPayload} to the specified slave with the
     * given reason and then closes its channel.
     *
     * @param slaveID The slave ID to disconnect.
     * @param reason  The human-readable reason that will be sent to the slave before disconnect.
     *
     * @apiNote
     * The channel is only closed AFTER the disconnect payload has been flushed
     * (using a Netty {@link ChannelFuture} listener), so the slave reliably
     * receives the reason. If no active channel exists for {@code slaveID}, the
     * connection is simply removed from the registry.
     */
    public void disconnectSlave(String slaveID, String reason) {
        info("Disconnecting slave: '"+slaveID+"' for reason:\n"+reason);
        ManualDisconnectionPayload payload = new ManualDisconnectionPayload(reason);
        Channel ch = CHILD_SERVERS.get(slaveID);
        if (ch != null && ch.isActive()) {
            // Wait for the disconnect notification to be flushed before closing the channel,
            // otherwise the slave may not receive the reason.
            ch.writeAndFlush(payload).addListener(future -> {
                ch.close();
                removeChildConnection(slaveID);
            });
        } else {
            removeChildConnection(slaveID);
        }
    }


    private static void info(String message) {
        ModUtilitiesMod.LOGGER.info("[MasterTCPServer]: "+message);
    }
    private static void error(String message) {
        ModUtilitiesMod.LOGGER.error("[MasterTCPServer]: "+message);
    }
    private static void error(String message, Throwable throwable) {
        ModUtilitiesMod.LOGGER.error("[MasterTCPServer]: "+message, throwable);
    }
    private static void warn(String message) {
        ModUtilitiesMod.LOGGER.warn("[MasterTCPServer]: "+message);
    }
    private static void debug(String message) {
        ModUtilitiesMod.LOGGER.debug("[MasterTCPServer]: "+message);
    }
}
