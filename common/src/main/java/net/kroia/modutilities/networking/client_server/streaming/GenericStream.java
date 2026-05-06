package net.kroia.modutilities.networking.client_server.streaming;

import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * This class is used to create stream objects
 * It defines the behavior of the stream, in which interval a packet gets sent for example
 * @param <CONTEXT_DATA> is the data that gets sent from the stream requestor to the stream provider.
 *
 * @param <DATA> is one data chunk of the stream.
 */
public abstract class GenericStream<CONTEXT_DATA, DATA> {
    private StreamManager manager = null;
    private UUID StreamID = null;
    private @Nullable UUID requestorPlayerUUID = null;
    private CONTEXT_DATA contextData = null;

    /**
     * Creates a copy of this stream. to provide a stream based storage for variables
     * in the derived class.
     * @apiNote
     * This methode gets called serverside and clientside.
     *
     * @return a new instance of this stream.
     */
    public abstract GenericStream<CONTEXT_DATA, DATA> copy();

    /**
     * Returns the unique identifier for the stream type.
     * This ID must be unique across all streams.
     * @apiNote
     * This methode gets called serverside and clientside.
     *
     * @return A string representing the request type ID.
     */
    public abstract String getStreamTypeID();

    /**
     * Returns true if the stream needs to be routed to the master server
     * If true, the stream path is: Client <-- ClientServer <-- MasterServer
     * If false, the stream path is: Client <-- ClientServer
     * @return true if the stream gets redirected to the master server
     */
    public boolean needsRoutingToMaster()
    {
        return false;
    }

    /**
     * Updates the stream on the server side once per tick.
     * This function is used to trigger the sending of the next stream packet.
     */
    protected void updateOnServer(){
        throw new AssertionError("updateOnServer must be implemented in the subclass of "+getStreamTypeID());
    }

    /**
     * Updates the stream on the client side once per tick.
     * This function is used to trigger the receiving of the next stream packet.
     */
    protected void updateOnClient(){
        throw new AssertionError("updateOnClient must be implemented in the subclass of "+getStreamTypeID());
    }

    /**
     * Sends the stream packet to the client.
     * This method is used to send the stream packet to the client.
     * It must be called from the server side.
     * @apiNote
     * This methode gets called serverside and clientside.
     */
    protected final void sendPacket()
    {
        if (manager == null) {
            throw new IllegalStateException("StreamManager is not set for this stream of "+getStreamTypeID());
        }
        manager.sendStreamPacket(StreamID);
    }

    /**
     * Stops the stream
     * @apiNote
     * This method gets called serverside and clientside.
     */
    protected final void stopStream()
    {
        if (manager == null) {
            throw new IllegalStateException("StreamManager is not set for this stream of "+getStreamTypeID());
        }
        manager.stopStream(StreamID);
    }


    /**
     * Called when the stream gets started on the client side. (Client will send stream data to the server)
     * @apiNote
     * This method gets called on the clientside.
     */
    public void onStartStreamSendingOnClient() {
        throw new AssertionError("onStartStreamSendingOnClient must be implemented in the subclass of "+getStreamTypeID());
    }

    /**
     * Called when the stream gets stopped on the client side. (Client will stop sending stream data to the server)
     * @apiNote
     * This method gets called on the clientside.
     */
    public void onStopStreamSendingOnClient() {
        throw new AssertionError("onStopStreamSendingOnClient must be implemented in the subclass of "+getStreamTypeID());
    }

    /**
     * Called when the stream gets started on the server side. (Server will send stream data to the client)
     * @apiNote
     * This method gets called on the serverside.
     */
    public void onStartStreamSendingOnSever() {
        throw new AssertionError("onStartStreamSendingOnSever must be implemented in the subclass of "+getStreamTypeID());
    }

    /**
     * Called when the stream gets stopped on the server side. (Server will stop sending stream data to the client)
     * @apiNote
     * This method gets called on the serverside.
     */
    public void onStopStreamSendingOnServer() {
        throw new AssertionError("onStopStreamSendingOnServer must be implemented in the subclass of "+getStreamTypeID());
    }

    /**
     * Provides the stream packet on the server side.
     * This method is used to provide the stream packet that will be sent to the client.
     * It must be implemented in the subclass of this stream if the stream is a server-side stream.
     * @apiNote
     * This methode gets called on the serverside.
     *
     * @return The stream packet data to be sent to the client.
     */

    public DATA provideStreamPacketOnServer()  {
        throw new AssertionError("provideStreamPacketOnServer must be implemented in the subclass of "+getStreamTypeID());
    }

    /**
     * Provides the stream packet on the client side.
     * This method is used to provide the stream packet that will be sent to the server.
     * It must be implemented in the subclass of this stream if the stream is a client-side stream.
     * @apiNote
     * This methode gets called on the clientside.
     *
     * @return The stream packet data to be sent to the server.
     */
    public DATA provideStreamPacketOnClient()  {
        throw new AssertionError("provideStreamPacketOnClient must be implemented in the subclass of "+getStreamTypeID());
    }


    /**
     * Encodes the context data for the stream.
     * This method is used to encode the context data that will be sent in the beginning of the stream.
     * It must be implemented in the subclass of this stream.
     * @apiNote
     * This methode gets called serverside and clientside.
     *
     * @param buffer The buffer to write the context data to.
     * @param context The context data to encode.
     */
    public abstract void encodeContextData(RegistryFriendlyByteBuf buffer, CONTEXT_DATA context);

    /**
     * Decodes the context data for the stream.
     * This method is used to decode the context data that was sent in the beginning of the stream.
     * It must be implemented in the subclass of this stream.
     * @apiNote
     * This methode gets called serverside and clientside.
     *
     * @param buffer The buffer to read the context data from.
     * @return The decoded context data.
     */
    public abstract CONTEXT_DATA decodeContextData(RegistryFriendlyByteBuf buffer);

    /**
     * Encodes the data for the stream packet.
     * This method is used to encode the data that will be sent in the stream packet.
     * It must be implemented in the subclass of this stream.
     * @apiNote
     * This methode gets called serverside and clientside.
     *
     * @param buffer The buffer to write the data to.
     * @param data The data to encode.
     */
    public abstract void encodeData(RegistryFriendlyByteBuf buffer, DATA data);

    /**
     * Decodes the data for the stream packet.
     * This method is used to decode the data that was sent in the stream packet.
     * It must be implemented in the subclass of this stream.
     * @apiNote
     * This methode gets called serverside and clientside.
     *
     * @param buffer The buffer to read the data from.
     * @return The decoded data.
     */
    public abstract DATA decodeData(RegistryFriendlyByteBuf buffer);


    /**
     * Returns the unique identifier for the stream.
     * @return The unique identifier for the stream.
     */
    public final UUID getStreamID()
    {
        return this.StreamID;
    }

    /**
     * Returns the UUID of the player that requested this stream data.
     * This is used to identify the player that requested the stream.
     * @return The UUID of the player that requested this stream, or null if it is a client->server stream.
     */
    public final @Nullable UUID getRequestorPlayerUUID()
    {
        return this.requestorPlayerUUID;
    }

    /**
     * Returns the context data for the stream.
     * This data is used to provide additional information about the stream.
     * It is set when the stream is started and can be used to provide context for the stream data.
     * @return The context data for the stream.
     */
    public final CONTEXT_DATA getContextData()
    {
        return this.contextData;
    }


    /**
     * Convenience method to start a server-to-client stream using this stream definition.
     * Equivalent to calling {@link StreamSystem#startServerToClientStream}.
     *
     * @param startData            The context data sent to the server with the start request.
     * @param streamDataHandler    The handler invoked on the client for each data chunk received.
     * @param streamStoppedHandler Optional handler invoked once the stream has stopped.
     *
     * @apiNote
     * This method must be called on the client side only.
     *
     * @return The unique stream UUID, used later with {@link StreamSystem#stopStream(UUID)}.
     */
    public UUID startServerToClient(@NotNull CONTEXT_DATA startData, @NotNull Consumer<DATA> streamDataHandler, @Nullable Runnable streamStoppedHandler)
    {
        return StreamSystem.startServerToClientStream(this, startData, streamDataHandler, streamStoppedHandler);
    }

    /*
     * ----------------------------------------------------------------------------------------------------
     *
     *      I N T E R N A L   M E T H O D S
     *
     * ----------------------------------------------------------------------------------------------------
     */


    /**
     * INTERNAL METHODE, DO NOT CALL THIS METHOD MANUALLY!
     *
     * Fills the buf with a new stream packet on the server side, by retrieving the next
     * data chunk via {@link #provideStreamPacketOnServer()} and encoding it.
     *
     * @param buf The buffer to write the stream packet data to.
     */
    public void createStreamPacketOnServer(RegistryFriendlyByteBuf buf)
    {
        try{
            encodeData(buf, provideStreamPacketOnServer());
        } catch (Exception e) {
            throw new RuntimeException("Error while encoding context data for stream packet on server. Stream: "+this, e);
        }
    }

    /**
     * INTERNAL METHODE, DO NOT CALL THIS METHOD MANUALLY!
     *
     * Fills the buf with a new stream packet on the client side, by retrieving the next
     * data chunk via {@link #provideStreamPacketOnClient()} and encoding it.
     *
     * @param buf The buffer to write the stream packet data to.
     */
    public void createStreamPacketOnClient(RegistryFriendlyByteBuf buf)
    {
        try{
            encodeData(buf, provideStreamPacketOnClient());
        } catch (Exception e) {
            throw new RuntimeException("Error while encoding context data for stream packet on client. Stream: "+this, e);
        }
    }


    public void updateInternalOnServer()
    {
        try{
            updateOnServer();
        } catch (Exception e) {
            throw new RuntimeException("Error while updating stream on server. Stream: "+this, e);
        }
    }
    public void updateInternalOnClient()
    {
        try{
            updateOnClient();
        } catch (Exception e) {
            throw new RuntimeException("Error while updating stream on client. Stream: "+this, e);
        }
    }


    public final void copyFrom(GenericStream<CONTEXT_DATA, DATA> other)
    {
        this.manager = other.manager;
        this.StreamID = other.StreamID;
    }
    public final void setStreamID(UUID streamID)
    {
        this.StreamID = streamID;
    }
    public final void setRequestorPlayerUUID(UUID requestorPlayerUUID)
    {
        this.requestorPlayerUUID = requestorPlayerUUID;
    }
    public final void setContextData(RegistryFriendlyByteBuf contextDataBuf)
    {
        try {
            this.contextData = decodeContextData(contextDataBuf);
        } catch (Exception e) {
            throw new RuntimeException("Error while decoding context data for stream: "+this, e);
        }
    }
    public void setManager(StreamManager manager)
    {
        this.manager = manager;
    }
    public StreamManager getManager()
    {
        return this.manager;
    }

    @Override
    public String toString() {

        return "GenericStream{" +
                "manager=" + manager +
                ", StreamID=" + StreamID +
                (requestorPlayerUUID!=null?", requestorPlayerUUID=" + requestorPlayerUUID:"") +
                ", streamTypeID='" + getStreamTypeID() + '\'' +
                '}';
    }
}
