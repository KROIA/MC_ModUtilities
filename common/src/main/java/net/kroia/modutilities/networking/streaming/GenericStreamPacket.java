package net.kroia.modutilities.networking.streaming;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.NetworkPacket;
import net.kroia.modutilities.networking.arrs.AsynchronousRequestResponseSystem;
import net.kroia.modutilities.networking.arrs.GenericResponsePacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.UUID;


/**
 * This Class is used by the "streaming system".
 * This packet is used to send a data chunk from client to server or vice versa.
 * It contains the stream ID and the data associated with the stream.
 */
public class GenericStreamPacket extends NetworkPacket {

    UUID streamID;
    FriendlyByteBuf data;

    public GenericStreamPacket(FriendlyByteBuf buf) {
        super(buf);
    }
    public GenericStreamPacket(UUID streamID, FriendlyByteBuf data) {
        super();
        this.streamID = streamID;
        this.data = data;
    }


    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(streamID);
        byte[] bytes = Arrays.copyOf(data.asByteBuf().array(), data.asByteBuf().readableBytes());
        buf.writeBytes(bytes);
    }

    @Override
    public void decode(FriendlyByteBuf buf) {
        this.streamID = buf.readUUID();

        int length = buf.readableBytes();
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        this.data = new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(bytes));
    }

    public UUID getStreamID() {
        return streamID;
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
