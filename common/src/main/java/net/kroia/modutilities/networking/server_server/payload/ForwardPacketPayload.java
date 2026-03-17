package net.kroia.modutilities.networking.server_server.payload;

import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record ForwardPacketPayload(
        UUID senderId,
        String senderServerID,
        String targetServerID,
        ResourceLocation packetType,
        byte[] data
) implements Payload {
    @Override
    public int packetId() { return PacketIds.FORWARD_PACKET; }

    /** Convenience: true when this is a broadcast (no specific target). */
    public boolean isBroadcast()
    {
        return targetServerID == null || targetServerID.isBlank();
    }
}