package net.kroia.modutilities.networking;

public interface INetworkPayloadEncoder {

    /**
     * Converts the packet data to bytes and writes it to the provided buffer.
     *
     * @param buf The buffer to write the packet data to.
     */
    void encode(net.minecraft.network.FriendlyByteBuf buf);
}
