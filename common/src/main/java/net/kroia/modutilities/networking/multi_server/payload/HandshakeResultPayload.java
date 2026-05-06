package net.kroia.modutilities.networking.multi_server.payload;

import io.netty.buffer.ByteBuf;
import net.kroia.modutilities.networking.multi_server.slave.SlaveServerClient;
import net.minecraft.network.codec.StreamCodec;

/**
 * Sent by the master in response to a {@link HandshakePayload} to inform the
 * slave whether its connection attempt was accepted.
 * <p>
 * The carried {@link SlaveServerClient.ConnectionEstablishState} indicates one of:
 * {@code SUCCESS}, {@code INVALID_SHARED_SECRET}, or {@code SLAVE_ID_ALREADY_USED}.
 *
 * @param result The outcome of the handshake authentication.
 */
public record HandshakeResultPayload(SlaveServerClient.ConnectionEstablishState result) implements Payload {
    /**
     * {@inheritDoc}
     *
     * @return {@link PacketIds#HANDSHAKE_RESULT}.
     */
    @Override
    public int packetId() { return PacketIds.HANDSHAKE_RESULT; }


    /**
     * {@link StreamCodec} that serializes the contained
     * {@link SlaveServerClient.ConnectionEstablishState} as its enum ordinal.
     *
     * @apiNote
     * Because the codec is ordinal-based, reordering or removing constants of
     * {@link SlaveServerClient.ConnectionEstablishState} would break wire
     * compatibility between master and slave.
     */
    public static final StreamCodec<ByteBuf, HandshakeResultPayload> STREAM_CODEC =
            StreamCodec.of((buf, result) -> buf.writeInt(result.result.ordinal()),
            (buf) -> new HandshakeResultPayload(SlaveServerClient.ConnectionEstablishState.class.getEnumConstants()[buf.readInt()])
    );
}
