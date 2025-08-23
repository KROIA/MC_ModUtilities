package net.kroia.modutilities.networking.streaming;

import net.kroia.modutilities.networking.NetworkPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.UUID;


/**
 * This Class is used by the "streaming system".
 * This packet is used to start a stream from client to server or vice versa.
 * It contains a stream ID, a stream type ID, and the context data associated with the stream.
 */
public class StreamStartPacket extends NetworkPacket {
    UUID streamID;
    String streamTypeID;
    FriendlyByteBuf data;


    public StreamStartPacket(FriendlyByteBuf buf) {
        super(buf);
    }
    public StreamStartPacket(String streamTypeID, FriendlyByteBuf data) {
        super();
        this.streamID = UUID.randomUUID();
        this.streamTypeID = streamTypeID;
        this.data = data;
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(streamID);
        buf.writeUtf(streamTypeID);
        byte[] bytes = Arrays.copyOf(data.asByteBuf().array(), data.asByteBuf().readableBytes());
        buf.writeBytes(bytes);
    }

    @Override
    public void decode(FriendlyByteBuf buf) {
        this.streamID = buf.readUUID();
        this.streamTypeID = buf.readUtf();
        int length = buf.readableBytes();
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        this.data = new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(bytes));
    }

    public UUID getStreamID() {
        return streamID;
    }
    public String getStreamTypeID() {
        return streamTypeID;
    }
    public FriendlyByteBuf getData() {
        return data;
    }


    @Override
    protected void handleOnClient() {
        StreamSystem.handlePacket(this);
    }

    @Override
    protected void handleOnServer(ServerPlayer sender) {
        StreamSystem.handlePacket(this, sender);
    }
}
