package net.kroia.modutilities.networking;

import dev.architectury.networking.NetworkChannel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class NetworkManager {

    public final NetworkChannel CHANNEL;

    public NetworkManager(String modID) {
        this(modID, "default_channel");
    }
    public NetworkManager(String modID, String channelName) {
        this.CHANNEL = NetworkChannel.create(new ResourceLocation(modID, channelName));
    }

    abstract public void setupClientReceiverPackets();

    abstract public void setupServerReceiverPackets();

    protected <T extends NetworkPacket> void register(Class<T> type,
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

    public void sendToServer(INetworkPacket packet) {
        CHANNEL.sendToServer(packet);
    }
    public void sendToClient(ServerPlayer receiver, INetworkPacket packet) {
        CHANNEL.sendToPlayer(receiver, packet);
    }
}
