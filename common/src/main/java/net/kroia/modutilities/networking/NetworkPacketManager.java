package net.kroia.modutilities.networking;



import dev.architectury.networking.NetworkManager;
import net.kroia.modutilities.networking.client_server.NetworkPacket;
import net.kroia.modutilities.networking.client_server.PacketHandler;
import net.kroia.modutilities.networking.client_server.arrs.AsynchronousRequestResponseSystem;
import net.kroia.modutilities.networking.client_server.streaming.StreamSystem;
import net.kroia.modutilities.networking.multi_server.ForwardPacketHandler;
import net.kroia.modutilities.networking.multi_server.MultiServerPacketRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Per-mod registration entry point for the MC_ModUtilities networking layer.
 * Mods extend this class and override {@link #setupClientReceiverPackets()},
 * {@link #setupServerReceiverPackets()} and {@link #setupServerServerPackets()}
 * to register their {@link NetworkPacket} types via the {@code registerS2C}, {@code registerC2S}
 * and {@code registerS2S} helpers.
 *
 * @apiNote
 * A single instance is typically created during mod construction. Optional subsystems such as the
 * Asynchronous Request/Response System and the Stream System can be enabled from the constructor
 * by calling {@link #setupARRS()} and/or {@link #setupStreamSystem()}.
 */
public abstract class NetworkPacketManager {


    /**
     * Creates a network packet manager bound to the given mod, using the default channel name.
     *
     * @param modID the namespace of the owning mod, typically used to derive packet type identifiers.
     */
    public NetworkPacketManager(String modID) {
        this(modID, "default_channel");

    }

    /**
     * Creates a network packet manager bound to the given mod and channel name.
     * <p>
     * Note: Channel isolation is not currently implemented. Packet types are
     * registered globally via Architectury's {@link NetworkManager}, so
     * {@code modID} and {@code channelName} are accepted for future use but
     * have no effect at this time.
     *
     * @param modID       the namespace of the owning mod.
     * @param channelName the network channel name to use for this manager.
     */
    public NetworkPacketManager(String modID, String channelName) {
        // Channel isolation is not implemented — all packet types share a single
        // global registry in Architectury's NetworkManager. The parameters are
        // retained in the signature for forward-compatibility but are intentionally
        // unused.
    }

    /**
     * Set up the Asynchronous Request Response System (ARRS) for this network manager.
     * This method can be called in the constructor of the derived NetworkManager class if the ARRS is used.
     */
    protected void setupARRS()
    {
        AsynchronousRequestResponseSystem.setup(this);
    }

    /**
     * Set up the Stream System for this network manager.
     * This method can be called in the constructor of the derived NetworkManager class if streaming is used.
     */
    protected void setupStreamSystem()
    {
        StreamSystem.setup(this);
    }

    /**
     * Registers all S2C (server-to-client) packet receivers handled by this mod.
     *
     * @apiNote
     * Called once on the client side during mod setup. Implementations should call
     * {@link #registerS2C(CustomPacketPayload.Type, StreamCodec)} (or one of its overloads) for each S2C packet type.
     */
    abstract public void setupClientReceiverPackets();

    /**
     * Registers all C2S (client-to-server) packet receivers handled by this mod.
     *
     * @apiNote
     * Called once on the server side during mod setup. Implementations should call
     * {@link #registerC2S(CustomPacketPayload.Type, StreamCodec)} (or one of its overloads) for each C2S packet type.
     */
    abstract public void setupServerReceiverPackets();

    /**
     * Registers all server-to-server packet types used by this mod for the multi-server relay system.
     *
     * @apiNote
     * Called once during multi-server setup. Implementations should call
     * {@link #registerS2S(CustomPacketPayload.Type, StreamCodec)} (or its overload) for each S2S packet type.
     */
    abstract public void setupServerServerPackets();

    /**
     * Registers a server-to-client packet type with a custom handler.
     *
     * @param packetType  the packet payload type identifier.
     * @param streamCodec the codec used to serialize/deserialize the packet on the wire.
     * @param handler     the handler invoked on the client when a packet of this type is received.
     * @param <T>         the concrete {@link NetworkPacket} subtype.
     */
    public <T extends NetworkPacket> void registerS2C(CustomPacketPayload.Type<T> packetType, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec, PacketHandler<? super T> handler) {

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, packetType, streamCodec, handler::handleClient);
        //NetworkManager.registerReceiver(NetworkManager.Side.C2S, packetType, streamCodec, handler::handleServer);
    }
    /**
     * Registers a server-to-client packet type with a custom handler and a multi-server forward handler.
     *
     * @param packetType     the packet payload type identifier.
     * @param streamCodec    the codec used to serialize/deserialize the packet on the wire.
     * @param handler        the handler invoked on the client when a packet of this type is received.
     * @param forwardHandler the handler used when the packet is relayed between master and slave servers.
     * @param <T>            the concrete {@link NetworkPacket} subtype.
     */
    public <T extends NetworkPacket> void registerS2C(CustomPacketPayload.Type<T> packetType, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec, PacketHandler<? super T> handler, ForwardPacketHandler<? super T> forwardHandler) {

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, packetType, streamCodec, handler::handleClient);
        MultiServerPacketRegistry.register(packetType, streamCodec, forwardHandler);
        //NetworkManager.registerReceiver(NetworkManager.Side.C2S, packetType, streamCodec, handler::handleServer);
    }

    /**
     * Registers a server-to-client packet type using {@link NetworkPacket#HANDLER} for both the client receiver
     * and the multi-server relay path.
     *
     * @param packetType  the packet payload type identifier.
     * @param streamCodec the codec used to serialize/deserialize the packet on the wire.
     * @param <T>         the concrete {@link NetworkPacket} subtype.
     */
    public <T extends NetworkPacket> void registerS2C(CustomPacketPayload.Type<T> packetType, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, packetType, streamCodec, NetworkPacket.HANDLER::handleClient);
        MultiServerPacketRegistry.register(packetType, streamCodec, NetworkPacket.HANDLER);
        //NetworkManager.registerReceiver(NetworkManager.Side.C2S, packetType, streamCodec, NetworkPacket.HANDLER::handleServer);
    }

    /**
     * Registers a client-to-server packet type with a custom handler.
     *
     * @param packetType  the packet payload type identifier.
     * @param streamCodec the codec used to serialize/deserialize the packet on the wire.
     * @param handler     the handler invoked on the server when a packet of this type is received.
     * @param <T>         the concrete {@link NetworkPacket} subtype.
     */
    public <T extends NetworkPacket> void registerC2S(CustomPacketPayload.Type<T> packetType, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec, PacketHandler<? super T> handler) {

        //NetworkManager.registerReceiver(NetworkManager.Side.S2C, packetType, streamCodec, handler::handleClient);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, packetType, streamCodec, handler::handleServer);
    }

    /**
     * Registers a client-to-server packet type with a custom handler and a multi-server forward handler.
     *
     * @param packetType     the packet payload type identifier.
     * @param streamCodec    the codec used to serialize/deserialize the packet on the wire.
     * @param handler        the handler invoked on the server when a packet of this type is received.
     * @param forwardHandler the handler used when the packet is relayed between master and slave servers.
     * @param <T>            the concrete {@link NetworkPacket} subtype.
     */
    public <T extends NetworkPacket> void registerC2S(CustomPacketPayload.Type<T> packetType, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec, PacketHandler<? super T> handler, ForwardPacketHandler<? super T> forwardHandler) {

        //NetworkManager.registerReceiver(NetworkManager.Side.S2C, packetType, streamCodec, handler::handleClient);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, packetType, streamCodec, handler::handleServer);
        MultiServerPacketRegistry.register(packetType, streamCodec, forwardHandler);
    }

    /**
     * Registers a client-to-server packet type using {@link NetworkPacket#HANDLER} for both the server receiver
     * and the multi-server relay path.
     *
     * @param packetType  the packet payload type identifier.
     * @param streamCodec the codec used to serialize/deserialize the packet on the wire.
     * @param <T>         the concrete {@link NetworkPacket} subtype.
     */
    public <T extends NetworkPacket> void registerC2S(CustomPacketPayload.Type<T> packetType, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {

        //NetworkManager.registerReceiver(NetworkManager.Side.S2C, packetType, streamCodec, NetworkPacket.HANDLER::handleClient);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, packetType, streamCodec, NetworkPacket.HANDLER::handleServer);
        MultiServerPacketRegistry.register(packetType, streamCodec, NetworkPacket.HANDLER);
    }

    /**
     * Registers a server-to-server packet type for the multi-server relay system, using {@link NetworkPacket#HANDLER}
     * as the forward handler.
     *
     * @param packetType  the packet payload type identifier.
     * @param streamCodec the codec used to serialize/deserialize the packet on the wire.
     * @param <T>         the concrete {@link NetworkPacket} subtype.
     */
    public <T extends NetworkPacket> void registerS2S(CustomPacketPayload.Type<T> packetType, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
        MultiServerPacketRegistry.register(packetType, streamCodec, NetworkPacket.HANDLER);
    }

    /**
     * Registers a server-to-server packet type for the multi-server relay system, using a custom forward handler.
     *
     * @param packetType     the packet payload type identifier.
     * @param streamCodec    the codec used to serialize/deserialize the packet on the wire.
     * @param forwardHandler the handler used when the packet is relayed between master and slave servers.
     * @param <T>            the concrete {@link NetworkPacket} subtype.
     */
    public <T extends NetworkPacket> void registerS2S(CustomPacketPayload.Type<T> packetType, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec, ForwardPacketHandler<? super T> forwardHandler) {
        MultiServerPacketRegistry.register(packetType, streamCodec, forwardHandler);
    }

    /**
     * Sends a network packet to the server.
     * This method can be used to send packets from the client to the server.
     *
     * @param packet The network packet to send.
     */
    public void sendToServer(NetworkPacket packet) {
        NetworkManager.sendToServer(packet);
    }

    /**
     * Sends a network packet to a specific client.
     * This method can be used to send packets from the server to a specific client.
     *
     * @param receiver The player who will receive the packet.
     * @param packet The network packet to send.
     */
    public void sendToClient(ServerPlayer receiver, NetworkPacket packet) {
        NetworkManager.sendToPlayer(receiver, packet);
    }

    /**
     * Sends a network packet from the server to a group of clients.
     *
     * @param players the players who will receive the packet.
     * @param packet  the network packet to send.
     */
    public void sendToClients(Iterable<ServerPlayer> players, NetworkPacket packet) {
        NetworkManager.sendToPlayers(players, packet);
    }
}
