package net.kroia.modutilities.networking.multi_server.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Sent by the master to a slave to signal that the master is intentionally
 * closing the connection, carrying a human-readable reason.
 * <p>
 * The slave should display or log {@link #reason()} and stop its reconnect
 * attempts (the disconnect is deliberate, not a transient network failure).
 *
 * @param reason A human-readable explanation for the disconnect.
 */
public record ManualDisconnectionPayload(String reason) implements Payload{
    /**
     * {@inheritDoc}
     *
     * @return {@link PacketIds#MANUAL_DISCONNECT}.
     */
    @Override
    public int packetId() {
        return PacketIds.MANUAL_DISCONNECT;
    }

    /** {@link StreamCodec} that serializes/deserializes the {@code reason} string as UTF-8. */
    public static final StreamCodec<ByteBuf, ManualDisconnectionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, o->o.reason,
            ManualDisconnectionPayload::new
    );
}
