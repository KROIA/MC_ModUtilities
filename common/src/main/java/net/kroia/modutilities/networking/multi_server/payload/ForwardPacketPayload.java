package net.kroia.modutilities.networking.multi_server.payload;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record ForwardPacketPayload(
        @Nullable UUID senderPlayerUUID,
        String senderServerID,
        ResourceLocation packetType,
        byte[] data
) implements Payload {
    @Override
    public int packetId() { return PacketIds.FORWARD_PACKET; }
}