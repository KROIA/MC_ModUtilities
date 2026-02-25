package net.kroia.modutilities.networking.streaming;

import net.kroia.modutilities.networking.NetworkPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * This Class is used by the "streaming system".
 * The packet is used to stop the stream or to notify the other side that the stream has stopped.
 */
public class StreamStopPacket extends NetworkPacket {
    UUID streamID;
    public StreamStopPacket(FriendlyByteBuf buf) {
        super(buf);
    }
    public StreamStopPacket(UUID streamID) {
        super();
        this.streamID = streamID;
    }


    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(streamID);
    }

    @Override
    public void decode(FriendlyByteBuf buf) {
        this.streamID = buf.readUUID();
    }

    public UUID getStreamID() {
        return streamID;
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
