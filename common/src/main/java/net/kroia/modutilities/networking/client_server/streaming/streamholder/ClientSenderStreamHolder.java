package net.kroia.modutilities.networking.client_server.streaming.streamholder;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.NetworkPacketManager;
import net.kroia.modutilities.networking.client_server.streaming.GenericStream;
import net.kroia.modutilities.networking.client_server.streaming.StreamStopClientSenderPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.UUID;

/**
 * This class is used to hold the data of a client->server stream, located on the client side.
 * @param <CONTEXT_DATA> The type of context data associated with the stream.
 * @param <DATA>         The type of data that the stream will handle.
 */
public class ClientSenderStreamHolder<CONTEXT_DATA, DATA>
{
    /**
     * The NetworkManager instance used to send packets to the server.
     */
    private final NetworkPacketManager networkManager;

    /**
     * A copy of the registered stream object.
     */
    public final GenericStream<CONTEXT_DATA, DATA> stream;

    /**
     * Flag to check if the stream should be removed.
     * This is used to prevent multiple calls to the stream stop method.
     */
    private boolean doRemove = false;

    /**
     * Creates a new ClientSenderStreamHolder tracking the client-side state of a client-to-server stream.
     * The given stream definition is copied so each active stream has its own state.
     *
     * @param networkManager The NetworkPacketManager used to send packets to the server.
     * @param stream         The registered stream definition (will be copied).
     * @param contextDataBuf The encoded context data used to initialize the copy.
     * @param streamID       The unique stream UUID identifying this stream instance.
     */
    public ClientSenderStreamHolder(NetworkPacketManager networkManager,
                                    GenericStream<CONTEXT_DATA, DATA> stream,
                                    RegistryFriendlyByteBuf contextDataBuf,
                                    UUID streamID) {
        this.networkManager = networkManager;
        this.stream = stream.copy();
        this.stream.copyFrom(stream);
        this.stream.setStreamID(streamID);
        this.stream.setRequestorPlayerUUID(null);
        this.stream.setContextData(contextDataBuf);
    }

    /**
     * Calls the update method of the stream object on the client side.
     */
    public void update()
    {
        stream.updateInternalOnClient();
    }

    /**
     * Stops the stream and sends a stop packet to the server for notification.
     */
    public void streamStop()
    {
        if(stream != null && !doRemove)
        {
            doRemove = true; // Mark for removal
            try {
                stream.onStopStreamSendingOnClient();
            }
            catch (Exception e) {
                error("Error while stopping stream: "+stream, e);
            }
            StreamStopClientSenderPacket stopPacket = new StreamStopClientSenderPacket(stream.getStreamID());
            networkManager.sendToServer(stopPacket);
        }

    }
    /**
     * @return True once {@link #streamStop()} has been called and this holder is queued for removal.
     */
    public boolean isDoRemove() {
        return doRemove;
    }
    private void error(String msg, Throwable e) {
        ModUtilitiesMod.LOGGER.error("[ClientSenderStreamHolder] " + msg, e);
    }
}