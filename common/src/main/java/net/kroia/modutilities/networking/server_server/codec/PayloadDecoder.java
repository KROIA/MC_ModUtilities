package net.kroia.modutilities.networking.server_server.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import net.kroia.modutilities.networking.ExtraCodecUtils;
import net.kroia.modutilities.networking.server_server.payload.*;
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
            case PacketIds.HANDSHAKE_RESULT ->  new HandshakeResultPayload(
                    ByteBufCodecs.BOOL.decode(in)
            );
            case PacketIds.BROADCAST -> new BroadcastPayload(
                    readString(in),  // senderName
                    readString(in),  // fromServer
                    readString(in)   // message
            );
            case PacketIds.FORWARD_PACKET -> new ForwardPacketPayload(
                    ExtraCodecUtils.nullable(UUIDUtil.STREAM_CODEC).decode(in),
                    ByteBufCodecs.STRING_UTF8.decode(in),
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