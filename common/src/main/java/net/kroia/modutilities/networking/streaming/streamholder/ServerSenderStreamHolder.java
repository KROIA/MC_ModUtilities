package net.kroia.modutilities.networking.streaming.streamholder;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.ServerPlayerUtilities;
import net.kroia.modutilities.networking.NetworkManager;
import net.kroia.modutilities.networking.streaming.GenericStream;
import net.kroia.modutilities.networking.streaming.StreamStopPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * This class is used to hold the data of a server->client stream, located on the server side.
 * @param <CONTEXT_DATA> The type of context data associated with the stream.
 * @param <DATA>         The type of data that the stream will handle.
 */
public class ServerSenderStreamHolder<CONTEXT_DATA, DATA>
{
    /**
     * The NetworkManager instance used to send packets to the target player.
     */
    private final NetworkManager networkManager;

    /**
     * A copy of the registered stream object.
     */
    public final GenericStream<CONTEXT_DATA, DATA> stream;

    /**
     * The UUID of the player that this stream is being sent to.
     */
    public final UUID playerUUID;

    /**
     * Flag to check if the stream should be removed.
     */
    private boolean doRemove = false;
    public ServerSenderStreamHolder(NetworkManager manager,
                                    GenericStream<CONTEXT_DATA, DATA> stream,
                                    FriendlyByteBuf contextDataBuf,
                                    UUID playerUUID,
                                    UUID streamID) {
        this.networkManager = manager;
        this.stream = stream.copy();
        this.stream.copyFrom(stream);
        this.stream.setStreamID(streamID);
        this.stream.setRequestorPlayerUUID(playerUUID);
        this.stream.setContextData(contextDataBuf);
        this.playerUUID = playerUUID;
    }

    /**
     * Calls the update methode of the stream object on the server side.
     */
    public void update()
    {
        stream.updateInternalOnServer();
    }

    /**
     * Stops the stream and sending a StreamStopPacket to the target player for notification.
     */
    public void streamEnd() {

        if(stream != null && !doRemove)
        {
            doRemove = true; // Mark for removal
            try {
                stream.onStopStreamSendingOnServer();
            }
            catch (Exception e) {
                error("Error while stopping stream: "+stream, e);
            }
            ServerPlayer targetPlayer = ServerPlayerUtilities.getOnlinePlayer(playerUUID);
            if (targetPlayer != null) {
                StreamStopPacket stopPacket = new StreamStopPacket(stream.getStreamID());
                networkManager.sendToClient(targetPlayer, stopPacket);
            }
        }
    }
    public boolean isDoRemove() {
        return doRemove;
    }
    private void error(String msg, Throwable e) {
        ModUtilitiesMod.LOGGER.error("[ServerSenderStreamHolder] " + msg, e);
    }
}