package net.kroia.modutilities.networking;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

public interface INetworkPacket {


    /**
     * Converts the packet data to bytes and writes it to the provided buffer.
     *
     * @param buf The buffer to write the packet data to.
     */
    void toBytes(FriendlyByteBuf buf);

    /**
     * Reads the packet data from the provided buffer and initializes the packet.
     *
     * @param buf The buffer to read the packet data from.
     */
    void fromBytes(FriendlyByteBuf buf);

    /**
     * Receives the packet and processes it in the context of the network manager.
     *
     * @param contextSupplier A supplier that provides the context for the packet.
     */
    void receive(Supplier<NetworkManager.PacketContext> contextSupplier);
}
