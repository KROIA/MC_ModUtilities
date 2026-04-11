package net.kroia.modutilities.networking.multi_server.slave;

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
import net.kroia.modutilities.networking.multi_server.MultiServerPacketRegistry;
import net.kroia.modutilities.networking.multi_server.codec.PayloadDecoder;
import net.kroia.modutilities.networking.multi_server.codec.PayloadEncoder;
import net.kroia.modutilities.networking.multi_server.payload.ForwardPacketPayload;
import net.kroia.modutilities.networking.multi_server.payload.HandshakePayload;
import net.kroia.modutilities.networking.multi_server.payload.Payload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class SlaveServerClient {
    public enum ConnectionEstablishState
    {
        SUCCESS,
        INVALID_SHARED_SECRET,
        SLAVE_ID_ALREADY_USED
    }


    private final String masterHost;
    private final int    masterPort;
    private String slaveIP = "";
    private final String serverId;
    private final String sharedSecret;
    private final MinecraftServer  mcServer;

    private final NioEventLoopGroup group;
    private Channel channel;
    private volatile boolean shuttingDown = false;

    private @Nullable Throwable connectionFailReason = null;

    private final Runnable onConnectionAccepted;
    private final Consumer<ConnectionEstablishState> onConnectionFailed;
    private final Consumer<Throwable> onConnectionLost;
    private final Runnable onDisconnect;

    private boolean masterDisconnected = false;
    private String masterDisconnectReason = "";

    public SlaveServerClient(MinecraftServer mcServer, String sharedSecret, String slaveServerID, String masterHostIP, int masterHostTcpPort,
                             Runnable  onConnectionAccepted, Consumer<ConnectionEstablishState> onConnectionFailed, Consumer<Throwable>  onConnectionLost, Runnable onDisconnect)
    {
        this.masterHost = masterHostIP;
        this.masterPort = masterHostTcpPort;
        this.serverId = slaveServerID;
        this.sharedSecret = sharedSecret;
        this.mcServer = mcServer;
        this.onConnectionAccepted = onConnectionAccepted;
        this.onConnectionFailed = onConnectionFailed;
        this.onConnectionLost = onConnectionLost;
        this.onDisconnect = onDisconnect;

        group = new NioEventLoopGroup();

    }

    // ── Connect / Disconnect ──────────────────────────────────────────────────

    public void connect()
    {
        if (shuttingDown)
            return;
        info("Connecting to " + masterHost + ":" + masterPort);

        SlaveServerClient client = this;
        Consumer<SlaveServerClient.ConnectionEstablishState> onConnectionSuccess = this::onConnectionEstablishedResult;
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
                                .addLast(new SlavePacketHandler(mcServer, client, onConnectionSuccess));
                    }
                });

        bootstrap.connect(masterHost, masterPort).addListener((ChannelFuture future) -> {
            if (future.isSuccess()) {
                channel = future.channel();
                connectionFailReason = null;

                // Get real outbound IP instead of relying on localAddress()
                try (Socket s = new Socket("8.8.8.8", 80)) {
                    slaveIP = s.getLocalAddress().getHostAddress();
                } catch (IOException e) {
                    slaveIP = "127.0.0.1"; // fallback
                }

                info("Connected to master at "+masterHost+":" + masterPort);
                // Immediately authenticate with the master
                sendToMaster(new HandshakePayload(serverId, sharedSecret));
            } else {
                connectionFailReason =  future.cause();
                warn("Could not connect to master — retrying in 5s... Reason: "+connectionFailReason);
                scheduleReconnect();
                onConnectionLost.accept(connectionFailReason);
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
        onDisconnect.run();
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
    public boolean isShuttingDown() {
        return shuttingDown;
    }

    public void onMasterDisconnected(String reason)
    {
        masterDisconnected = true;
        masterDisconnectReason = reason;
        disconnect();
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    /**
     * Send any {@link Payload} to the master.
     * Thread-safe — Netty queues the write internally.
     */
    public boolean sendToMaster(@Nullable UUID senderPlayerUUID, CustomPacketPayload packet) {
        //info("Sending packet: '"+packet.type().id()+"' to master for player '" + senderPlayerUUID+"'");
        ForwardPacketPayload payload = MultiServerPacketRegistry.createForwardPacketPayload(senderPlayerUUID, serverId, packet);
        return sendToMaster(payload);
    }
    public boolean sendToMaster(Payload payload) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(payload).addListener(f -> {
                if (!f.isSuccess()) {
                    error("Failed to send "+payload.getClass().getSimpleName()+" to master", f.cause());
                }
            });
        } else {
            warn("Cannot send — not connected to master.");
            return false;
        }
        return true;
    }

    public String getServerID() { return serverId; }
    public String getSlaveIP()
    {
        return slaveIP;
    }
    public String getMasterIP()
    {
        return masterHost;
    }
    public int getMasterPort()
    {
        return masterPort;
    }
    public boolean masterHasDisconnected()
    {
        return masterDisconnected;
    }
    public String getMasterDisconnectReason()
    {
        return masterDisconnectReason;
    }

    private void onConnectionEstablishedResult(ConnectionEstablishState state) {
        switch(state)
        {
            case ConnectionEstablishState.SUCCESS ->  {
                info("Master accepts connection");
                try {
                    onConnectionAccepted.run();
                }catch (Exception e) {
                    error("Failed to call callback: onConnectionAccepted. Reason: "+e.getMessage(),e);
                }
            }
            case ConnectionEstablishState.INVALID_SHARED_SECRET -> {
                info("Can't connect to master. The shared secret does not match wit the secret from the server.");
                try {
                    onConnectionFailed.accept(state);
                }catch (Exception e) {
                    error("Failed to call callback: onConnectionFailed. Reason: "+e.getMessage(),e);
                }
            }
            case ConnectionEstablishState.SLAVE_ID_ALREADY_USED -> {
                info("Can't connect to master. A slave with the same slave ID: '"+getServerID()+"' is already connected to the master.");
                try {
                    onConnectionFailed.accept(state);
                }catch (Exception e) {
                    error("Failed to call callback: onConnectionFailed. Reason: "+e.getMessage(),e);
                }
            }
        }
    }

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
