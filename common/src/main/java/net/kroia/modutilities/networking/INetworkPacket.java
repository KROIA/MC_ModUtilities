package net.kroia.modutilities.networking;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

public interface INetworkPacket {


    void toBytes(FriendlyByteBuf buf);

    void fromBytes(FriendlyByteBuf buf);

    void receive(Supplier<NetworkManager.PacketContext> contextSupplier);
}
