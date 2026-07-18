package net.kroia.modutilities.networking.multi_server.slave;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * TCP client side of the multi-server protocol that connects this Minecraft
 * server (the "slave") to a remote master server.
 * <p>
 * Wraps a Netty {@link io.netty.bootstrap.Bootstrap} and pipeline configured to
 * frame, encode/decode, and dispatch {@link Payload} messages exchanged with the
 * master. Performs an initial handshake using a shared secret, exposes
 * {@link #sendToMaster(Payload)} for outbound traffic, and automatically
 * schedules reconnection attempts via {@link #scheduleReconnect()} if the
 * connection drops.
 *
 * @apiNote
 * Lifecycle callbacks ({@code onConnectionAccepted}, {@code onConnectionFailed},
 * {@code onConnectionLost}, {@code onDisconnect}) are invoked from Netty I/O
 * threads. Implementations that touch Minecraft world state must hop to the
 * server thread themselves (e.g. via {@code MinecraftServer#execute}).
 */
public class SlaveServerClient {
    /**
     * Possible outcomes of an attempted handshake against the master server.
     * Returned to the slave inside a {@link net.kroia.modutilities.networking.multi_server.payload.HandshakeResultPayload}.
     */
    public enum ConnectionEstablishState
    {
        /** The master accepted the slave's credentials and the connection is established. */
        SUCCESS,
        /** The shared secret presented by the slave does not match the master's configuration. */
        INVALID_SHARED_SECRET,
        /** Another slave is already registered with the same {@code serverId}. */
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

    /**
     * Creates a new slave client without yet opening a connection.
     * Call {@link #connect()} to actually dial the master.
     *
     * @param mcServer             The Minecraft server instance, used to schedule callbacks on the main thread and access registry data.
     * @param sharedSecret         The shared secret used during the handshake to authenticate against the master.
     * @param slaveServerID        Unique identifier of this slave; must not collide with any other connected slave.
     * @param masterHostIP         Hostname or IP address of the master server.
     * @param masterHostTcpPort    TCP port the master is listening on.
     * @param onConnectionAccepted Callback fired once the master has accepted the handshake.
     * @param onConnectionFailed   Callback fired if the handshake is rejected, receiving the {@link ConnectionEstablishState} reason.
     * @param onConnectionLost     Callback fired when an existing or pending connection drops, receiving the underlying {@link Throwable}.
     * @param onDisconnect         Callback fired when the local {@link #disconnect()} method completes shutdown.
     *
     * @apiNote
     * All callbacks are invoked from Netty event-loop threads.
     */
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

    /**
     * Asynchronously opens the TCP connection to the master and immediately
     * sends a {@link HandshakePayload} on success.
     * <p>
     * On failure, schedules a reconnect attempt via {@link #scheduleReconnect()}
     * and invokes the {@code onConnectionLost} callback with the cause.
     *
     * @apiNote
     * This method returns immediately; the actual connect happens on a Netty
     * worker thread. If the client is currently shutting down (see
     * {@link #disconnect()}), the call is a no-op.
     */
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
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
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
                /*try (Socket s = new Socket("8.8.8.8", 80)) {
                    slaveIP = s.getLocalAddress().getHostAddress();
                } catch (IOException e) {
                    slaveIP = "127.0.0.1"; // fallback
                }*/
                try {
                    slaveIP = ((InetSocketAddress) future.channel().localAddress())
                            .getAddress().getHostAddress();
                } catch (Exception e) {
                    warn("Failed to determine slave IP from local address: " + e.getMessage());
                    slaveIP = "";
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

    /**
     * Schedules another {@link #connect()} attempt after a fixed delay (5 seconds).
     * Does nothing if the client is in the middle of shutting down.
     *
     * @apiNote
     * Called automatically when a connect fails or the channel becomes inactive;
     * may also be called manually if a higher-level component wants to force a
     * reconnect cycle.
     */
    public void scheduleReconnect() {
        if (!shuttingDown) {
            group.schedule(this::connect, 5, TimeUnit.SECONDS);
        }
    }

    /**
     * Closes the active channel (if any), tears down the Netty event-loop
     * group, and invokes the {@code onDisconnect} callback.
     * <p>
     * Once called, further reconnect attempts are suppressed: the client is
     * considered dead and a new {@link SlaveServerClient} must be created to
     * reconnect.
     */
    public void disconnect() {
        shuttingDown = true;
        if (channel != null) channel.close();
        group.shutdownGracefully();
        connectionFailReason = null;
        info("Disconnected from master.");
        onDisconnect.run();
    }

    /**
     * @return {@code true} if a Netty channel is open and active to the master,
     *         {@code false} otherwise.
     */
    public boolean isConnected() {
        return channel != null && channel.isActive();
    }

    /**
     * @return {@code true} if the most recent connect attempt failed and no
     *         successful connection has been re-established since.
     */
    public boolean isConnectionFailed() {
        return connectionFailReason != null;
    }
    /**
     * @return The {@link Throwable} that caused the most recent connect
     *         failure, or {@code null} if the last attempt succeeded.
     */
    public @Nullable Throwable getConnectionFailReason() {
        return connectionFailReason;
    }
    /**
     * @return {@code true} once {@link #disconnect()} has been called, after
     *         which reconnect attempts are suppressed.
     */
    public boolean isShuttingDown() {
        return shuttingDown;
    }

    /**
     * Records that the master has explicitly disconnected this slave and
     * triggers a local shutdown.
     *
     * @param reason A human-readable explanation supplied by the master.
     *
     * @apiNote
     * Called by {@link SlavePacketHandler} when a
     * {@link net.kroia.modutilities.networking.multi_server.payload.ManualDisconnectionPayload}
     * is received.
     */
    public void onMasterDisconnected(String reason)
    {
        masterDisconnected = true;
        masterDisconnectReason = reason;
        disconnect();
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    /**
     * Wraps a {@link CustomPacketPayload} in a {@link ForwardPacketPayload} and
     * sends it to the master for relay.
     *
     * @param senderPlayerUUID UUID of the originating player, or {@code null} if the sender is the server itself.
     * @param packet           The Minecraft custom packet payload to forward.
     * @return {@code true} if the packet was queued for sending; {@code false} if no active connection was available.
     *
     * @apiNote
     * Thread-safe — Netty queues to write internally.
     */
    public boolean sendToMaster(@Nullable UUID senderPlayerUUID, CustomPacketPayload packet) {
        //info("Sending packet: '"+packet.type().id()+"' to master for player '" + senderPlayerUUID+"'");
        ForwardPacketPayload payload = MultiServerPacketRegistry.createForwardPacketPayload(senderPlayerUUID, packet);
        return sendToMaster(payload);
    }
    /**
     * Sends an arbitrary {@link Payload} to the master.
     *
     * @param payload The payload to transmit.
     * @return {@code true} if the payload was queued for sending (not a guarantee of successful
     *         delivery); {@code false} if no active connection was available.
     *
     * @apiNote
     * Thread-safe — Netty queues to write internally. Delivery failures are
     * logged via the channel write listener but do not affect the return value.
     */
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

    /** @return The unique identifier this slave registered with the master. */
    public String getServerID() { return serverId; }
    /**
     * @return The local IP address the Netty channel is bound to, or an empty
     *         string if it could not be determined or no connection has been
     *         established yet.
     */
    public String getSlaveIP()
    {
        return slaveIP;
    }
    /** @return The hostname or IP address of the master this client is configured for. */
    public String getMasterIP()
    {
        return masterHost;
    }
    /** @return The TCP port of the master this client is configured for. */
    public int getMasterPort()
    {
        return masterPort;
    }
    /**
     * @return {@code true} if the master has previously sent a
     *         {@link net.kroia.modutilities.networking.multi_server.payload.ManualDisconnectionPayload}
     *         to this slave.
     */
    public boolean masterHasDisconnected()
    {
        return masterDisconnected;
    }
    /**
     * @return The reason string supplied by the master in its most recent
     *         manual disconnect, or an empty string if no manual disconnect has
     *         occurred.
     */
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
