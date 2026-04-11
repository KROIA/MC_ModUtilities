package net.kroia.modutilities.networking.multi_server.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ManualDisconnectionPayload(String reason) implements Payload{
    @Override
    public int packetId() {
        return PacketIds.MANUAL_DISCONNECT;
    }

    public static final StreamCodec<ByteBuf, ManualDisconnectionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, o->o.reason,
            ManualDisconnectionPayload::new
    );
}
