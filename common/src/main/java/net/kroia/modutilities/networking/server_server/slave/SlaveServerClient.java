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
import net.kroia.modutilities.networking.server_server.codec.PayloadDecoder;
import net.kroia.modutilities.networking.server_server.codec.PayloadEncoder;
import net.kroia.modutilities.networking.server_server.payload.HandshakePayload;
import net.kroia.modutilities.networking.server_server.payload.Payload;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.TimeUnit;

public class SlaveServerClient {
    private final String hubHost;
    private final int    hubPort;
    private final String serverId;
    private final String sharedSecret;
    private final MinecraftServer  mcServer;

    private final NioEventLoopGroup group;
    private Channel channel;
    private volatile boolean shuttingDown = false;

    public SlaveServerClient(MinecraftServer mcServer, String sharedSecret, String slaveServerID, String masterHostIP, int masterHostTcpPort)
    {
        this.hubHost = masterHostIP;
        this.hubPort = masterHostTcpPort;
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
        info("Connecting to " + hubHost + ":" + hubPort);

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
                                // ③ bytes → HubPayload
                                .addLast(new PayloadDecoder())
                                // ④ HubPayload → bytes
                                .addLast(new PayloadEncoder())
                                // ⑤ Handle packets received FROM the hub
                                .addLast(new SlavePacketHandler(mcServer, client));
                    }
                });

        bootstrap.connect(hubHost, hubPort).addListener((ChannelFuture future) -> {
            if (future.isSuccess()) {
                channel = future.channel();
                info("Connected to hub at "+hubHost+":" + hubPort);
                // Immediately authenticate with the hub
                sendToHub(new HandshakePayload(serverId, sharedSecret));
            } else {
                warn("Could not connect to hub — retrying in 5s...");
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
        info("Disconnected from hub.");
    }

    public boolean isConnected() {
        return channel != null && channel.isActive();
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    /**
     * Send any {@link Payload} to the hub.
     * Thread-safe — Netty queues the write internally.
     */
    public void sendToHub(Payload payload) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(payload).addListener(f -> {
                if (!f.isSuccess()) {
                    error("Failed to send "+payload.getClass().getSimpleName()+" to hub", f.cause());
                }
            });
        } else {
            warn("Cannot send — not connected to hub.");
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
