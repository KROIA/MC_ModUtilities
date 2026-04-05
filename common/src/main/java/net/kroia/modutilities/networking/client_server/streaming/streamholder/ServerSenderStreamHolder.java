package net.kroia.modutilities.networking.client_server.streaming.streamholder;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.ServerPlayerUtilities;
import net.kroia.modutilities.networking.client_server.ClientServerPacketManager;
import net.kroia.modutilities.networking.client_server.streaming.GenericStream;
import net.kroia.modutilities.networking.client_server.streaming.StreamStopServerSenderPacket;
import net.kroia.modutilities.networking.server_server.ServerServerManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

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
    private final ClientServerPacketManager networkManager;

    /**
     * A copy of the registered stream object.
     */
    public final GenericStream<CONTEXT_DATA, DATA> stream;

    /**
     * The UUID of the player that this stream is being sent to.
     */
    public final UUID playerUUID;

    /**
     * The Slave server id if the request is started from a slave server and
     * the packets need to be routed to the slave server.
     */
    @Nullable
    private final String slaveServerID;

    /**
     * Flag to check if the stream should be removed.
     */
    private boolean doRemove = false;



    public ServerSenderStreamHolder(ClientServerPacketManager manager,
                                    GenericStream<CONTEXT_DATA, DATA> stream,
                                    RegistryFriendlyByteBuf contextDataBuf,
                                    UUID playerUUID,
                                    UUID streamID,
                                    @Nullable String slaveServerID) {
        this.networkManager = manager;
        this.stream = stream.copy();
        this.stream.copyFrom(stream);
        this.stream.setStreamID(streamID);
        this.stream.setRequestorPlayerUUID(playerUUID);
        this.stream.setContextData(contextDataBuf);
        this.playerUUID = playerUUID;
        this.slaveServerID = slaveServerID;
    }

    /**
     * Calls the update methode of the stream object on the server side.
     */
    public void update()
    {
        stream.updateInternalOnServer();
    }

    /**
     * Stops the stream and sending a StreamStopServerSenderPacket to the target player for notification.
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
            StreamStopServerSenderPacket stopPacket = new StreamStopServerSenderPacket(stream.getStreamID());
            if(ServerServerManager.isRunning() && ServerServerManager.isMaster() && needsRoutingToSlaveServer())
            {
                ServerServerManager.sendToSlave(slaveServerID, stopPacket);
            }
            else
            {
                ServerPlayer targetPlayer = ServerPlayerUtilities.getOnlinePlayer(playerUUID);
                if (targetPlayer != null) {

                    networkManager.sendToClient(targetPlayer, stopPacket);
                }
            }
        }
    }
    public boolean isDoRemove() {
        return doRemove;
    }
    public boolean needsRoutingToSlaveServer()
    {
        return slaveServerID != null && !slaveServerID.isEmpty();
    }
    public  @Nullable String getSlaveServerID()
    {
        return slaveServerID;
    }
    private void error(String msg) {
        ModUtilitiesMod.LOGGER.error("[ServerSenderStreamHolder] " + msg);
    }
    private void error(String msg, Throwable e) {
        ModUtilitiesMod.LOGGER.error("[ServerSenderStreamHolder] " + msg, e);
    }
}