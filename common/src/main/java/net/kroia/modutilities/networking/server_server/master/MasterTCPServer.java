package net.kroia.modutilities.networking.server_server.master;

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
import net.kroia.modutilities.networking.server_server.ServerServerPacketRegistry;
import net.kroia.modutilities.networking.server_server.codec.PayloadDecoder;
import net.kroia.modutilities.networking.server_server.codec.PayloadEncoder;
import net.kroia.modutilities.networking.server_server.payload.ForwardPacketPayload;
import net.kroia.modutilities.networking.server_server.payload.Payload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class MasterTCPServer {


    /** All currently connected child servers, keyed by their serverId. */
    private final Map<String, Channel> CHILD_SERVERS = new ConcurrentHashMap<>();

    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private Channel serverChannel;
    private final String sharedSecret;
    private final MinecraftServer mcServer;
    private final int tcpPort;

    private @Nullable Throwable startupFailReason = null;

    private final Runnable onServerStartSuccess;
    private final Consumer<Throwable> onServerStartFailure;
    private final Consumer<String> onSlaveConnected;
    private final Consumer<String> onSlaveDisconnected;


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
     * Starts the TCP listener on {@code port}.
     * Safe to call from the MC server thread — Netty runs on its own threads.
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
                info("TCP listener started on port "+ tcpPort);
                onServerStartSuccess.run();
            } else {
                startupFailReason = future.cause();
                error("Failed to bind TCP port: "+ tcpPort, startupFailReason);
                onServerStartFailure.accept(startupFailReason);
            }
        });
    }

    public void stop() {
        if (serverChannel != null) serverChannel.close();
        if (bossGroup  != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        startupFailReason =  null;
        info("TCP listener stopped.");
    }

    // ── Routing helpers ───────────────────────────────────────────────────────

    /** Send a payload to one specific child server. */
    public boolean sendToSlave(@Nullable UUID senderPlayerUUID, String targetServerID, CustomPacketPayload packet) {
        ForwardPacketPayload payload = ServerServerPacketRegistry.createForwardPacketPayload(senderPlayerUUID,"", packet);
        return sendToSlave(targetServerID, payload);
    }

    /** Broadcast a payload to all connected child servers. */
    public void broadcastToSlaves(@Nullable UUID senderPlayerUUID, CustomPacketPayload packet) {
        ForwardPacketPayload payload = ServerServerPacketRegistry.createForwardPacketPayload(senderPlayerUUID, "", packet);
        broadcastToSlaves(payload);
    }

    /** Broadcast to all children EXCEPT the one with {@code excludeServerId}. */
    public void broadcastToSlaves(@Nullable UUID senderPlayerUUID, CustomPacketPayload packet, String excludeServerId) {
        ForwardPacketPayload payload = ServerServerPacketRegistry.createForwardPacketPayload(senderPlayerUUID, "", packet);
        broadcastToSlaves(payload, excludeServerId);
    }

    public void broadcastToSlaves(@Nullable UUID senderPlayerUUID, CustomPacketPayload packet, List<String> excludeServerIds) {
        ForwardPacketPayload payload = ServerServerPacketRegistry.createForwardPacketPayload(senderPlayerUUID, "", packet);
        broadcastToSlaves(payload, excludeServerIds);
    }



    /** Send a payload to one specific child server. */
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

    /** Broadcast a payload to all connected child servers. */
    public void broadcastToSlaves(Payload payload) {
        CHILD_SERVERS.forEach((id, ch) -> {
            ch.writeAndFlush(payload);
        });
    }

    /** Broadcast to all children EXCEPT the one with {@code excludeServerId}. */
    public void broadcastToSlaves(Payload payload, String excludeServerId) {
        CHILD_SERVERS.forEach((id, ch) -> {
            if (!id.equals(excludeServerId) && ch.isActive()) {
                ch.writeAndFlush(payload);
            }
        });
    }

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
        CHILD_SERVERS.remove(serverId);
        info("Child server '"+serverId+"' disconnected.");
        onSlaveDisconnected.accept(serverId);
    }


    public boolean isRunning() {
        return serverChannel != null && serverChannel.isActive();
    }
    public boolean isStartupFailed() {
        return startupFailReason != null;
    }
    public @Nullable Throwable getStartupFailReason() {
        return startupFailReason;
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
