package net.kroia.modutilities.networking.multi_server.payload;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Wraps an arbitrary {@code NetworkPacket} (encoded as bytes) for relay between
 * a master server and a slave server.
 * <p>
 * The byte payload is the serialized form of a registered packet; the receiver
 * uses {@link #packetType()} to look up the appropriate handler and then decodes
 * {@link #data()} back into the original packet object.
 *
 * @param senderPlayerUUID The UUID of the player on whose behalf the packet is sent, or {@code null} if the sender is a server.
 * @param senderServerID   The ID of the server that originally sent the packet.
 * @param packetType       The {@link ResourceLocation} identifying the wrapped packet type.
 * @param data             The raw encoded bytes of the wrapped packet.
 */
public record ForwardPacketPayload(
        @Nullable UUID senderPlayerUUID,
        String senderServerID,
        ResourceLocation packetType,
        byte[] data
) implements Payload {
    /**
     * {@inheritDoc}
     *
     * @return {@link PacketIds#FORWARD_PACKET}.
     */
    @Override
    public int packetId() { return PacketIds.FORWARD_PACKET; }
}
