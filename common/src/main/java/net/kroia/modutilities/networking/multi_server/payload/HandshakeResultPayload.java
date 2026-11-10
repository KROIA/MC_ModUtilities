package net.kroia.modutilities.networking.multi_server.payload;

import io.netty.buffer.ByteBuf;
import net.kroia.modutilities.networking.multi_server.slave.SlaveServerClient;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Sent by the master in response to a {@link HandshakePayload} to inform the
 * slave whether its connection attempt was accepted.
 * <p>
 * The carried {@link SlaveServerClient.ConnectionEstablishState} indicates one of:
 * {@code SUCCESS}, {@code INVALID_SHARED_SECRET}, {@code SLAVE_ID_ALREADY_USED},
 * {@code MC_VERSION_MISMATCH} or {@code DUPLICATE_SERVER_UID}.
 *
 * @param result    The outcome of the handshake authentication.
 * @param masterUid The master's server uid (diagnostics only). Sent only on {@code SUCCESS} to slaves that
 *                  sent their own uid, so older slaves never receive the trailing field; otherwise null.
 */
public record HandshakeResultPayload(SlaveServerClient.ConnectionEstablishState result, @Nullable UUID masterUid) implements Payload {
    /** Result without a master uid. */
    public HandshakeResultPayload(SlaveServerClient.ConnectionEstablishState result) {
        this(result, null);
    }

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
     * compatibility between master and slave. The optional {@code masterUid} trails the
     * state and is only written when non-null.
     */
    public static final StreamCodec<ByteBuf, HandshakeResultPayload> STREAM_CODEC =
            StreamCodec.of((buf, result) -> {
                        buf.writeInt(result.result.ordinal());
                        if (result.masterUid != null)
                            UUIDUtil.STREAM_CODEC.encode(buf, result.masterUid);
                    },
            (buf) -> new HandshakeResultPayload(SlaveServerClient.ConnectionEstablishState.class.getEnumConstants()[buf.readInt()],
                    buf.isReadable() ? UUIDUtil.STREAM_CODEC.decode(buf) : null) // masterUid, absent from older masters
    );
}
