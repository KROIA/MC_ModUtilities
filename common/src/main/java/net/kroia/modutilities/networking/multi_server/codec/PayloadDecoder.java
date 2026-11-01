package net.kroia.modutilities.networking.multi_server.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import net.kroia.modutilities.networking.ExtraCodecUtils;
import net.kroia.modutilities.networking.multi_server.payload.*;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Converts raw bytes received over TCP back into {@link Payload} objects.
 *
 * This decoder runs AFTER {@code LengthFieldBasedFrameDecoder}, which means
 * by the time this runs the {@code ByteBuf} already contains exactly one
 * complete frame — no need to worry about partial reads.
 */
public class PayloadDecoder extends ByteToMessageDecoder {

    /**
     * Decodes a single framed message into a {@link Payload} and adds it to {@code out}.
     * <p>
     * The first byte of the frame is interpreted as the packet ID and used to
     * select the correct {@link Payload} subtype to construct.
     *
     * @param ctx The Netty channel handler context for this connection.
     * @param in  The byte buffer holding exactly one complete frame to decode.
     * @param out The list to which the decoded {@link Payload} is appended for
     *            the next handler in the pipeline.
     *
     * @throws DecoderException If the leading packet ID byte does not correspond
     *                          to any known {@link Payload} subtype.
     *
     * @apiNote
     * Runs on the Netty I/O thread. Returns immediately if the buffer is empty.
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (!in.isReadable()) return;

        int packetId = in.readByte() & 0xFF;

        Payload payload = switch (packetId)
        {
            case PacketIds.HANDSHAKE -> new HandshakePayload(
                    readString(in),  // serverId
                    readString(in)   // token
            );
            case PacketIds.HANDSHAKE_RESULT ->  HandshakeResultPayload.STREAM_CODEC.decode(in);
            case PacketIds.BROADCAST -> new BroadcastPayload(
                    readString(in),  // senderName
                    readString(in),  // fromServer
                    readString(in)   // message
            );
            case PacketIds.MANUAL_DISCONNECT ->  ManualDisconnectionPayload.STREAM_CODEC.decode(in);
            case PacketIds.FORWARD_PACKET -> new ForwardPacketPayload(
                    ExtraCodecUtils.nullable(UUIDUtil.STREAM_CODEC).decode(in),
                    ResourceLocation.STREAM_CODEC.decode(in),
                    ByteBufCodecs.BYTE_ARRAY.decode(in)
            );
            default -> throw new DecoderException(
                    "Unknown hub packet ID: 0x" + Integer.toHexString(packetId));
        };

        out.add(payload); // passes decoded object to the next pipeline handler
    }

    /**
     * Reads a UTF-8 string written by {@link PayloadEncoder#writeString}.
     */
    private String readString(ByteBuf buf) {
        int length = buf.readShort() & 0xFFFF;
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}