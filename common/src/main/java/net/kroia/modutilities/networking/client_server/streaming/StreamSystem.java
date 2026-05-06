package net.kroia.modutilities.networking.client_server.streaming;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.NetworkPacketManager;
import net.kroia.modutilities.networking.multi_server.MultiServerManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The StreamSystem is used to start and stop a continuous stream of data form the server to the client or vice versa.
 * This class combines the components required for the Streaming system including the StreamRegistry and StreamManager.
 */
public class StreamSystem {

    /**
     * The StreamRegistry instance that holds all registered streams.
     */
    private static final StreamRegistry REGISTRY = new StreamRegistry();

    /**
     * The StreamManager instance that manages the streams.
     * This is initialized in the setup method and used to start and stop streams.
     */
    private static StreamManager STREAM_MANAGER;

    /**
     * Sets up the StreamSystem with the provided NetworkManager.
     * This method initializes the StreamManager and registers the necessary packets for stream handling.
     * Re-invoking this method replaces any existing StreamManager so the system survives an
     * integrated-server restart (e.g. opening a new singleplayer world in the same JVM).
     * Previously registered streams are re-bound to the new manager.
     *
     * @param networkManager The NetworkManager to use for packet handling.
     *
     * @apiNote
     * Must be called once on the client and once on the server side during mod initialization.
     */
    public static void setup(@NotNull NetworkPacketManager networkManager) {
        // Replace any existing manager so the system survives an integrated-server
        // restart (e.g. opening a new singleplayer world in the same JVM).
        STREAM_MANAGER = new StreamManager(networkManager);

        Map<String, StreamRegistry.RegistryData<?,?>> requests = REGISTRY.getRegistry();
        for(Map.Entry<String, StreamRegistry.RegistryData<?,?>> entry : requests.entrySet())
        {
            GenericStream<?, ?> request = entry.getValue().stream;
            if(request != null)
                request.setManager(STREAM_MANAGER);
        }

        // Register packets for streaming
        networkManager.registerS2C(GenericStreamPacket.TYPE, GenericStreamPacket.STREAM_CODEC, GenericStreamPacket.HANDLER, GenericStreamPacket.HANDLER);
        networkManager.registerC2S(StreamStartPacket.TYPE, StreamStartPacket.STREAM_CODEC, StreamStartPacket.HANDLER, StreamStartPacket.HANDLER);
        networkManager.registerC2S(StreamStopClientSenderPacket.TYPE, StreamStopClientSenderPacket.STREAM_CODEC, StreamStopClientSenderPacket.HANDLER, StreamStopClientSenderPacket.HANDLER);
        networkManager.registerS2C(StreamStopServerSenderPacket.TYPE, StreamStopServerSenderPacket.STREAM_CODEC, StreamStopServerSenderPacket.HANDLER, StreamStopServerSenderPacket.HANDLER);
    }

    /**
     * Registers a Stream in the registry.
     * @param stream The static Stream instance to register.
     * @return The registered stream, or null if there is already a stream with the same StreamTypeID registered.
     * @param <CONTEXT_DATA> The type of context data associated with the stream.
     *                       The context data gets sent to the stream provider and can contain additional data for the provider.
     * @param <DATA>         The type of data that the stream will handle.
     */
    public static <CONTEXT_DATA, DATA> GenericStream<CONTEXT_DATA, DATA> register(@NotNull GenericStream<CONTEXT_DATA, DATA> stream) {
        stream.setManager(STREAM_MANAGER);
        return REGISTRY.register(stream);
    }

    /**
     * Unregisters a Stream from the registry by its type ID.
     * @param stream The Stream instance to unregister.
     */
    public static <CONTEXT_DATA, DATA> void unregister(@NotNull GenericStream<CONTEXT_DATA, DATA> stream) {
        stream.setManager(null);
        REGISTRY.unregister(stream);
    }

    /**
     * Unregisters a Stream from the registry by its type ID.
     * @param streamTypeID The type ID of the stream to unregister.
     */
    public static void unregister(@NotNull String streamTypeID)
    {
        var request = REGISTRY.getRegisteredStream(streamTypeID);
        if(request != null) {
            request.setManager(null);
            REGISTRY.unregister(streamTypeID);
        }
    }

    /**
     * Clears the StreamRegistry.
     * This method is used to clear all registered streams.
     * It is useful for resetting the registry, for example, during mod reloads.
     */
    public static void clearRegistry() {
        var streams = REGISTRY.getRegistry();
        for(Map.Entry<String, StreamRegistry.RegistryData<?,?>> entry : streams.entrySet())
        {
            GenericStream<?, ?> stream = entry.getValue().stream;
            if(stream != null)
                stream.setManager(null);
        }
        REGISTRY.clear();
    }

    /**
     * Gets the StreamRegistry instance.
     * @param streamTypeID The type ID of the stream to retrieve.
     *                     This is defined in the implementation of the Stream objects getStreamTypeID() method.
     * @return The StreamRegistry instance.
     */
    public static GenericStream<?, ?> getRegisteredStream(@NotNull String streamTypeID) {
        return REGISTRY.getRegisteredStream(streamTypeID);
    }

    /**
     * Starts a server-to-client stream.
     * This method can only be called on the client side.
     * @param stream The registered stream object used for the stream.
     * @param contextData The context data to be sent to the server to start the stream.
     * @param streamHandler The handler that will be called when stream data is received from the server.
     * @param streamStoppedHandler An optional handler that will be called when the stream is stopped.
     * @return The stream UUID that needs to be saved to be able to stop the stream later.
     * @param <CONTEXT_DATA> The type of context data associated with the stream.
     * @param <DATA>         The type of data that the stream will handle.
     */
    public static <CONTEXT_DATA, DATA> UUID startServerToClientStream(
            @NotNull GenericStream<CONTEXT_DATA, DATA> stream,
            @NotNull CONTEXT_DATA contextData,
            @NotNull Consumer<DATA> streamHandler,
            @Nullable Runnable streamStoppedHandler) {
        checkManagerExists();
        return STREAM_MANAGER.startServerToClientStream(stream, contextData, streamHandler, streamStoppedHandler);
    }

    /**
     * Starts a client-to-server stream.
     * This method can only be called on the server side.
     * @param stream The registered stream object used for the stream.
     * @param contextData The context data to be sent to the client to start the stream.
     * @param streamHandler The handler that will be called when stream data is received from the client.
     * @param streamStoppedHandler An optional handler that will be called when the stream is stopped.
     * @param targetPlayer The player that the stream is being sent to.
     * @return The stream UUID that needs to be saved to be able to stop the stream later.
     * @param <CONTEXT_DATA> The type of context data associated with the stream.
     * @param <DATA>         The type of data that the stream will handle.
     */
    /*
    // Only Streams from server to client are supported
    public static <CONTEXT_DATA, DATA> UUID startClientToServerStream(
            @NotNull GenericStream<CONTEXT_DATA, DATA> stream,
            @NotNull CONTEXT_DATA contextData,
            @NotNull BiConsumer<DATA, ServerPlayer> streamHandler,
            @Nullable Runnable streamStoppedHandler,
            @NotNull ServerPlayer targetPlayer) {
        checkManagerExists();
        return STREAM_MANAGER.startClientToServerStream(stream, contextData, streamHandler, streamStoppedHandler, targetPlayer);
    }*/

    /**
     * Stops a stream with the given stream ID.
     * This method can be called on both the client and server side.
     * This method is safe to call from within the tick update of the stopping stream.
     *
     * @param streamID The unique stream ID returned by one of the start* methods.
     */
    public static void stopStream(@NotNull UUID streamID)
    {
        checkManagerExists();
        STREAM_MANAGER.stopStream(streamID);
    }

    /**
     * Gets a list of all active client->server streams.
     * The list holds tuples of
     *      - UUID: the unique stream ID
     *      - String: the stream type ID that can be used in the registry to get the stream object.
     * @return A list of UUIDs of the active server sender streams.
     */
    public List<Tuple<UUID, String>> getActiveServerSenderStreams()
    {
        checkManagerExists();
        return STREAM_MANAGER.getActiveServerSenderStreams();
    }

    /**
     * Gets a list of all active client->server streams.
     * The list holds tuples of
     *      - UUID: the unique stream ID
     *      - String: the stream type ID that can be used in the registry to get the stream object.
     * @return A list of UUIDs of the active client sender streams.
     */
    public List<Tuple<UUID, String>> getActiveClientSenderStreams()
    {
        checkManagerExists();
        return STREAM_MANAGER.getActiveClientSenderStreams();
    }

    /**
     * Gets a list of all active server->client streams.
     * The list holds tuples of
     *      - UUID: the unique stream ID
     *      - String: the stream type ID that can be used in the registry to get the stream object.
     * @return A list of UUIDs of the active server receiver streams.
     */
    public List<Tuple<UUID, String>> getActiveServerReceiverStreams()
    {
        checkManagerExists();
        return STREAM_MANAGER.getActiveServerReceiverStreams();
    }

    /**
     * Gets a list of all active client->server streams.
     * The list holds tuples of
     *      - UUID: the unique stream ID
     *      - String: the stream type ID that can be used in the registry to get the stream object.
     * @return A list of UUIDs of the active client receiver streams.
     */
    public List<Tuple<UUID, String>> getActiveClientReceiverStreams()
    {
        checkManagerExists();
        return STREAM_MANAGER.getActiveClientReceiverStreams();
    }



    /**
     * INTERNAL METHODE, DO NOT CALL THIS METHOD MANUALLY!
     *
     * Handles a StreamStartPacket on the server side. If the stream type is not registered
     * (mod version mismatch), a warning is logged and the packet is ignored. If the stream
     * needs routing to the master server and this node is a slave, the start request is
     * forwarded to the master via the MultiServerManager; otherwise a new server-sender
     * stream is started locally.
     *
     * @param packet           The StreamStartPacket to handle.
     * @param slaveServerID    The ID of the slave server that originated the request,
     *                         or null when handled directly by the destination server.
     * @param targetPlayerUUID The UUID of the player that requested the stream.
     */
    public static void handlePacket(StreamStartPacket packet, String slaveServerID, UUID targetPlayerUUID)
    {
        UUID streamID = packet.getStreamID();
        String StreamType = packet.getStreamTypeID();
        RegistryFriendlyByteBuf buf = packet.getData();
        var stream = REGISTRY.getRegisteredStream(StreamType);
        if (stream == null) {
            ModUtilitiesMod.LOGGER.warn("[StreamSystem] Received StreamStartPacket for unregistered stream type '" + StreamType + "' (mod version mismatch?), ignoring");
            return;
        }
        if(MultiServerManager.isRunning() && MultiServerManager.isSlave() && stream.needsRoutingToMaster())
        {
            STREAM_MANAGER.startRedirectedServerSenderStream(stream, streamID, buf, targetPlayerUUID);
            ModUtilitiesMod.LOGGER.info("[StreamSystem] Redirecting packet: "+packet.streamTypeID+" to master");
            MultiServerManager.sendToMaster(targetPlayerUUID,  packet);
        }
        else {
            STREAM_MANAGER.startServerSenderStream(stream, streamID, slaveServerID, buf, targetPlayerUUID);
        }
    }


    /**
     * INTERNAL METHODE, DO NOT CALL THIS METHOD MANUALLY!
     *
     * Handles a StreamStartPacket on the client side.
     * Currently a no-op since only server-to-client streams are supported.
     *
     * @param packet The StreamStartPacket to handle.
     */
    public static void handlePacket(StreamStartPacket packet)
    {
       /* UUID streamID = packet.getStreamID();
        String StreamType = packet.getStreamTypeID();
        RegistryFriendlyByteBuf buf = packet.getData();
        var stream = REGISTRY.getRegisteredStream(StreamType);
        STREAM_MANAGER.startClientSenderStream(stream, streamID, buf);*/
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
     * Handles a StreamStopClientSenderPacket on the client side.
     * @param packet The StreamStopClientSenderPacket to handle.
     */
    public static void handlePacketOnClient(StreamStopServerSenderPacket packet)
    {
        STREAM_MANAGER.handlePacketOnClient(packet);
    }

    /**
     * INTERNAL METHODE, DO NOT CALL THIS METHOD MANUALLY!
     *
     * Handles a StreamStopServerSenderPacket that was redirected from the master server
     * back to a slave server, so the slave can forward it to the originating client.
     *
     * @param packet The StreamStopServerSenderPacket to handle.
     */
    public static void handleRedirectedPacket(StreamStopServerSenderPacket packet)
    {
        STREAM_MANAGER.handlePacketOnServer(packet);
    }

    /**
     * INTERNAL METHODE, DO NOT CALL THIS METHOD MANUALLY!
     *
     * Handles a StreamStopClientSenderPacket on the server side.
     * @param packet The StreamStopClientSenderPacket to handle.
     */
    public static void handlePacket(StreamStopClientSenderPacket packet)
    {
        STREAM_MANAGER.handlePacketOnServer(packet);
    }


    /**
     * INTERNAL METHODE, DO NOT CALL THIS METHOD MANUALLY!
     *
     * Handles a GenericStreamPacket on the client side.
     * @param packet The GenericStreamPacket to handle.
     */
    public static void handlePacketOnClient(GenericStreamPacket packet)
    {
        STREAM_MANAGER.handlePacketOnClient(packet);
    }

    /**
     * INTERNAL METHODE, DO NOT CALL THIS METHOD MANUALLY!
     *
     * Handles a GenericStreamPacket that was redirected from the master server to a slave server,
     * forwarding the data chunk on to the originating client.
     *
     * @param packet The GenericStreamPacket to redirect.
     */
    public static void handleRedirectedPacket(GenericStreamPacket packet)
    {
        STREAM_MANAGER.redirectToClient(packet);
    }

    /**
     * INTERNAL METHODE, DO NOT CALL THIS METHOD MANUALLY!
     *
     * Handles a GenericStreamPacket on the server side.
     * @param packet The GenericStreamPacket to handle.
     * @param sender The player that sent the packet.
     */
    public static void handlePacket(GenericStreamPacket packet, ServerPlayer sender)
    {
        //STREAM_MANAGER.handlePacketOnServer(packet, sender);
    }

    private static void checkManagerExists()
    {
        if(STREAM_MANAGER == null)
            throw new IllegalStateException("""
                    StreamManager is not set. Cannot send request to server.
                    Make sure to setup the Streaming system correctly.
                    Call StreamSystem.setup() once on the client and server side.
                    Also register the Stream classes with StreamSystem.register(new CustomStream()) once on the client and server side.
                    """);
    }
}
