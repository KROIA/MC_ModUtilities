package net.kroia.modutilities.networking;

public interface INetworkPayloadDecoder {

    /**
     * Reads the packet data from the provided buffer and initializes the packet.
     *
     * @param buf The buffer to read the packet data from.
     */
    void decode(net.minecraft.network.FriendlyByteBuf buf);
}
