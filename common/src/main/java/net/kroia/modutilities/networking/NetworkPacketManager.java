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

public abstract class NetworkPacketManager {


    public NetworkPacketManager(String modID) {
        this(modID, "default_channel");

    }
    public NetworkPacketManager(String modID, String channelName) {

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

    abstract public void setupClientReceiverPackets();

    abstract public void setupServerReceiverPackets();

    abstract public void setupServerServerPackets();

    /**
     * Registers a network packet type with the given encoder, decoder, and message consumer.
     * The encoder is used to serialize the packet data into a FriendlyByteBuf,
     * the decoder is used to deserialize the packet data from a FriendlyByteBuf,
     * and the message consumer is used to handle the received packet.
     *
     */
    public <T extends NetworkPacket> void registerS2C(CustomPacketPayload.Type<T> packetType, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec, PacketHandler<? super T> handler) {

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, packetType, streamCodec, handler::handleClient);
        //NetworkManager.registerReceiver(NetworkManager.Side.C2S, packetType, streamCodec, handler::handleServer);
    }
    public <T extends NetworkPacket> void registerS2C(CustomPacketPayload.Type<T> packetType, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec, PacketHandler<? super T> handler, ForwardPacketHandler<? super T> forwardHandler) {

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, packetType, streamCodec, handler::handleClient);
        MultiServerPacketRegistry.register(packetType, streamCodec, forwardHandler);
        //NetworkManager.registerReceiver(NetworkManager.Side.C2S, packetType, streamCodec, handler::handleServer);
    }
    public <T extends NetworkPacket> void registerS2C(CustomPacketPayload.Type<T> packetType, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, packetType, streamCodec, NetworkPacket.HANDLER::handleClient);
        MultiServerPacketRegistry.register(packetType, streamCodec, NetworkPacket.HANDLER);
        //NetworkManager.registerReceiver(NetworkManager.Side.C2S, packetType, streamCodec, NetworkPacket.HANDLER::handleServer);
    }

    public <T extends NetworkPacket> void registerC2S(CustomPacketPayload.Type<T> packetType, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec, PacketHandler<? super T> handler) {

        //NetworkManager.registerReceiver(NetworkManager.Side.S2C, packetType, streamCodec, handler::handleClient);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, packetType, streamCodec, handler::handleServer);
    }
    public <T extends NetworkPacket> void registerC2S(CustomPacketPayload.Type<T> packetType, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec, PacketHandler<? super T> handler, ForwardPacketHandler<? super T> forwardHandler) {

        //NetworkManager.registerReceiver(NetworkManager.Side.S2C, packetType, streamCodec, handler::handleClient);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, packetType, streamCodec, handler::handleServer);
        MultiServerPacketRegistry.register(packetType, streamCodec, forwardHandler);
    }
    public <T extends NetworkPacket> void registerC2S(CustomPacketPayload.Type<T> packetType, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {

        //NetworkManager.registerReceiver(NetworkManager.Side.S2C, packetType, streamCodec, NetworkPacket.HANDLER::handleClient);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, packetType, streamCodec, NetworkPacket.HANDLER::handleServer);
        MultiServerPacketRegistry.register(packetType, streamCodec, NetworkPacket.HANDLER);
    }

    public <T extends NetworkPacket> void registerS2S(CustomPacketPayload.Type<T> packetType, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
        MultiServerPacketRegistry.register(packetType, streamCodec, NetworkPacket.HANDLER);
    }
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

    public void sendToClients(Iterable<ServerPlayer> players, NetworkPacket packet) {
        NetworkManager.sendToPlayers(players, packet);
    }
}
