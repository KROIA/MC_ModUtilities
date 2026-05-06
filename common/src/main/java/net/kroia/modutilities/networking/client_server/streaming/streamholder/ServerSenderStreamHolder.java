package net.kroia.modutilities.networking.client_server.streaming.streamholder;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.ServerPlayerUtilities;
import net.kroia.modutilities.networking.NetworkPacketManager;
import net.kroia.modutilities.networking.client_server.streaming.GenericStream;
import net.kroia.modutilities.networking.client_server.streaming.StreamStopServerSenderPacket;
import net.kroia.modutilities.networking.multi_server.MultiServerManager;
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
    private final NetworkPacketManager networkManager;

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



    /**
     * Creates a new ServerSenderStreamHolder tracking the server-side state of a server-to-client stream.
     * The given stream definition is copied so each active stream has its own state.
     *
     * @param manager        The NetworkPacketManager used to send packets to the target player.
     * @param stream         The registered stream definition (will be copied).
     * @param contextDataBuf The encoded context data used to initialize the copy.
     * @param playerUUID     The UUID of the player receiving the stream data.
     * @param streamID       The unique stream UUID identifying this stream instance.
     * @param slaveServerID  Optional slave server ID, set when packets need to be routed
     *                       through a slave server in a multi-server setup; null otherwise.
     */
    public ServerSenderStreamHolder(NetworkPacketManager manager,
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
            if(MultiServerManager.isRunning() && MultiServerManager.isMaster() && needsRoutingToSlaveServer())
            {
                MultiServerManager.sendToSlave(slaveServerID, stopPacket);
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
    /**
     * @return True once {@link #streamEnd()} has been called and this holder is queued for removal.
     */
    public boolean isDoRemove() {
        return doRemove;
    }

    /**
     * @return True if outgoing stream packets must be routed via a slave server before reaching the client.
     */
    public boolean needsRoutingToSlaveServer()
    {
        return slaveServerID != null && !slaveServerID.isEmpty();
    }

    /**
     * @return The slave server ID through which packets are routed, or null if no routing is required.
     */
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