package net.kroia.modutilities.networking;

import dev.architectury.networking.NetworkChannel;
import net.kroia.modutilities.networking.arrs.AsynchronousRequestResponseSystem;
import net.kroia.modutilities.networking.streaming.StreamSystem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class NetworkManager {

    private final NetworkChannel CHANNEL;

    public NetworkManager(String modID) {
        this(modID, "default_channel");

    }
    public NetworkManager(String modID, String channelName) {
        this.CHANNEL = NetworkChannel.create(new ResourceLocation(modID, channelName));
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

    /**
     * Registers a network packet type with the given encoder, decoder, and message consumer.
     * The encoder is used to serialize the packet data into a FriendlyByteBuf,
     * the decoder is used to deserialize the packet data from a FriendlyByteBuf,
     * and the message consumer is used to handle the received packet.
     *
     * @param type The class of the network packet type.
     * @param encoder The function to encode the packet data into a FriendlyByteBuf.
     * @param decoder The function to decode the packet data from a FriendlyByteBuf.
     * @param messageConsumer The consumer that handles the received packet.
     */
    public <T extends NetworkPacket> void register(Class<T> type,
                                BiConsumer<T, FriendlyByteBuf> encoder,
                                Function<FriendlyByteBuf, T> decoder,
                                BiConsumer<T, Supplier<dev.architectury.networking.NetworkManager.PacketContext>> messageConsumer) {


        CHANNEL.register(type, encoder, (buf)->{return decode(buf, decoder);}, messageConsumer);
    }



    private <T extends NetworkPacket> T decode(FriendlyByteBuf buf, Function<FriendlyByteBuf, T> decoder)
    {
        T obj = decoder.apply(buf);
        obj.setManager(this);
        return obj;
    }


    /**
     * Sends a network packet to the server.
     * This method can be used to send packets from the client to the server.
     *
     * @param packet The network packet to send.
     */
    public void sendToServer(INetworkPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    /**
     * Sends a network packet to a specific client.
     * This method can be used to send packets from the server to a specific client.
     *
     * @param receiver The player who will receive the packet.
     * @param packet The network packet to send.
     */
    public void sendToClient(ServerPlayer receiver, INetworkPacket packet) {
        CHANNEL.sendToPlayer(receiver, packet);
    }
}
