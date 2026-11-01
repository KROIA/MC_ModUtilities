package net.kroia.modutilities.networking.multi_server.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.kroia.modutilities.networking.ExtraCodecUtils;
import net.kroia.modutilities.networking.multi_server.payload.*;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;

/**
 * Converts a {@link Payload} Java object into raw bytes to send over TCP.
 */
public class PayloadEncoder extends MessageToByteEncoder<Payload> {

    /**
     * Encodes a {@link Payload} into the given byte buffer.
     * <p>
     * The packet ID returned by {@link Payload#packetId()} is always written
     * first, followed by the type-specific fields. The matching
     * {@link PayloadDecoder} reads back the packet ID to select the correct
     * decoding path.
     *
     * @param ctx     The Netty channel handler context for this connection.
     * @param payload The payload to encode.
     * @param out     The byte buffer to write the encoded representation into.
     *
     * @apiNote
     * Runs on the Netty I/O thread. The {@link io.netty.handler.codec.LengthFieldPrepender}
     * earlier in the pipeline prepends the frame length, so this method only
     * needs to write the payload contents itself.
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, Payload payload, ByteBuf out) {
        // Always write the packet ID first
        out.writeByte(payload.packetId());

        switch (payload) {
            case HandshakePayload hs -> {
                writeString(out, hs.serverId());
                writeString(out, hs.token());
            }
            case HandshakeResultPayload hr -> HandshakeResultPayload.STREAM_CODEC.encode(out, hr);
            case BroadcastPayload bc -> {
                writeString(out, bc.senderName());
                writeString(out, bc.fromServer());
                writeString(out, bc.message());
            }
            case ManualDisconnectionPayload dp -> ManualDisconnectionPayload.STREAM_CODEC.encode(out, dp);
            case ForwardPacketPayload bb -> {
                ExtraCodecUtils.nullable(UUIDUtil.STREAM_CODEC).encode(out, bb.senderPlayerUUID());
                ResourceLocation.STREAM_CODEC.encode(out, bb.packetType());
                ByteBufCodecs.BYTE_ARRAY.encode(out, bb.data());
            }
        }
    }

    /**
     * Writes a UTF-8 string as [2-byte unsigned length][bytes].
     * Max string length: 65535 bytes.
     */
    private void writeString(ByteBuf buf, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        buf.writeShort(bytes.length);
        buf.writeBytes(bytes);
    }
}