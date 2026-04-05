package net.kroia.modutilities.networking.server_server.slave;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.server_server.ServerServerPacketRegistry;
import net.kroia.modutilities.networking.server_server.codec.PayloadDecoder;
import net.kroia.modutilities.networking.server_server.codec.PayloadEncoder;
import net.kroia.modutilities.networking.server_server.payload.ForwardPacketPayload;
import net.kroia.modutilities.networking.server_server.payload.HandshakePayload;
import net.kroia.modutilities.networking.server_server.payload.Payload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SlaveServerClient {
    private final String masterHost;
    private final int    masterPort;
    private final String serverId;
    private final String sharedSecret;
    private final MinecraftServer  mcServer;

    private final NioEventLoopGroup group;
    private Channel channel;
    private volatile boolean shuttingDown = false;

    private @Nullable Throwable connectionFailReason = null;

    public SlaveServerClient(MinecraftServer mcServer, String sharedSecret, String slaveServerID, String masterHostIP, int masterHostTcpPort)
    {
        this.masterHost = masterHostIP;
        this.masterPort = masterHostTcpPort;
        this.serverId = slaveServerID;
        this.sharedSecret = sharedSecret;
        this.mcServer = mcServer;

        group = new NioEventLoopGroup();
    }

    // ── Connect / Disconnect ──────────────────────────────────────────────────

    public void connect()
    {
        if (shuttingDown)
            return;
        info("Connecting to " + masterHost + ":" + masterPort);

        SlaveServerClient client = this;
        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                // ① Frame splitter
                                .addLast(new LengthFieldBasedFrameDecoder(1 << 21, 0, 3, 0, 3))
                                // ② Frame length prepender
                                .addLast(new LengthFieldPrepender(3))
                                // ③ bytes → masterPayload
                                .addLast(new PayloadDecoder())
                                // ④ masterPayload → bytes
                                .addLast(new PayloadEncoder())
                                // ⑤ Handle packets received FROM the master
                                .addLast(new SlavePacketHandler(mcServer, client));
                    }
                });

        bootstrap.connect(masterHost, masterPort).addListener((ChannelFuture future) -> {
            if (future.isSuccess()) {
                channel = future.channel();
                connectionFailReason = null;
                info("Connected to master at "+masterHost+":" + masterPort);
                // Immediately authenticate with the master
                sendToMaster(new HandshakePayload(serverId, sharedSecret));
            } else {
                connectionFailReason =  future.cause();
                warn("Could not connect to master — retrying in 5s... Reason: "+connectionFailReason);
                scheduleReconnect();
            }
        });
    }

    public void scheduleReconnect() {
        if (!shuttingDown) {
            group.schedule(this::connect, 5, TimeUnit.SECONDS);
        }
    }

    public void disconnect() {
        shuttingDown = true;
        if (channel != null) channel.close();
        group.shutdownGracefully();
        connectionFailReason = null;
        info("Disconnected from master.");
    }

    public boolean isConnected() {
        return channel != null && channel.isActive();
    }

    public boolean isConnectionFailed() {
        return connectionFailReason != null;
    }
    public @Nullable Throwable getConnectionFailReason() {
        return connectionFailReason;
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    /**
     * Send any {@link Payload} to the master.
     * Thread-safe — Netty queues the write internally.
     */
    public void sendToMaster(@Nullable UUID packetIdentifier, @Nullable UUID senderPlayerUUID, CustomPacketPayload packet) {
        ForwardPacketPayload payload = ServerServerPacketRegistry.createForwardPacketPayload(packetIdentifier, senderPlayerUUID, serverId, packet);
        sendToMaster(payload);
    }
    public void sendToMaster(@Nullable UUID senderPlayerUUID, CustomPacketPayload packet) {
        info("Sending packet: '"+packet.type().id()+"' to master for player '" + senderPlayerUUID+"'");
        ForwardPacketPayload payload = ServerServerPacketRegistry.createForwardPacketPayload(null, senderPlayerUUID, serverId, packet);
        sendToMaster(payload);
    }
    public void sendToMaster(Payload payload) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(payload).addListener(f -> {
                if (!f.isSuccess()) {
                    error("Failed to send "+payload.getClass().getSimpleName()+" to master", f.cause());
                }
            });
        } else {
            warn("Cannot send — not connected to master.");
        }
    }

    public String getServerId() { return serverId; }


    private static void info(String message) {
        ModUtilitiesMod.LOGGER.info("[SlaveServerClient]: "+message);
    }
    private static void error(String message) {
        ModUtilitiesMod.LOGGER.error("[SlaveServerClient]: "+message);
    }
    private static void error(String message, Throwable throwable) {
        ModUtilitiesMod.LOGGER.error("[SlaveServerClient]: "+message, throwable);
    }
    private static void warn(String message) {
        ModUtilitiesMod.LOGGER.warn("[SlaveServerClient]: "+message);
    }
    private static void debug(String message) {
        ModUtilitiesMod.LOGGER.debug("[SlaveServerClient]: "+message);
    }
}
