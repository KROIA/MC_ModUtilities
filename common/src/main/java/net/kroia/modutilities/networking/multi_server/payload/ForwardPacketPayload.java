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
 * <p>
 * The identity of the originating server is <b>no longer carried on the wire</b>.
 * It is derived on the receiving side from the authenticated connection: the
 * master stamps the handshake {@code serverId} bound to the socket, and a slave
 * stamps the constant {@code "master"} (its only authenticated upstream). This
 * prevents an authenticated peer from spoofing its identity on a per-packet basis.
 *
 * @param senderPlayerUUID The UUID of the player on whose behalf the packet is sent, or {@code null} if the sender is a server.
 * @param packetType       The {@link ResourceLocation} identifying the wrapped packet type.
 * @param data             The raw encoded bytes of the wrapped packet.
 */
public record ForwardPacketPayload(
        @Nullable UUID senderPlayerUUID,
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
