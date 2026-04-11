package net.kroia.modutilities.networking.multi_server.payload;

import io.netty.buffer.ByteBuf;
import net.kroia.modutilities.networking.multi_server.slave.SlaveServerClient;
import net.minecraft.network.codec.StreamCodec;

public record HandshakeResultPayload(SlaveServerClient.ConnectionEstablishState result) implements Payload {
    @Override
    public int packetId() { return PacketIds.HANDSHAKE_RESULT; }



    public static final StreamCodec<ByteBuf, HandshakeResultPayload> STREAM_CODEC =
            StreamCodec.of((buf, result) -> buf.writeInt(result.result.ordinal()),
            (buf) -> new HandshakeResultPayload(SlaveServerClient.ConnectionEstablishState.class.getEnumConstants()[buf.readInt()])
    );
}
