package net.kroia.modutilities.networking.streaming;

import dev.architectury.event.events.common.TickEvent;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.ServerPlayerUtilities;
import net.kroia.modutilities.UtilitiesPlatform;
import net.kroia.modutilities.networking.PacketManager;
import net.kroia.modutilities.networking.streaming.streamholder.ClientReceiverStreamHolder;
import net.kroia.modutilities.networking.streaming.streamholder.ClientSenderStreamHolder;
import net.kroia.modutilities.networking.streaming.streamholder.ServerReceiverStreamHolder;
import net.kroia.modutilities.networking.streaming.streamholder.ServerSenderStreamHolder;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * This class is used to manage streams on the server and client side.
 * It keeps track of the active streams and provides methods to start and stop streams.
 */
public class StreamManager {

    /**
     * The NetworkManager that is used to send and receive stream packets.
     */
    private final PacketManager networkManager;

    /**
     * Maps to hold the active streams on the server and client side.
     * The keys are the stream IDs, and the values are the stream holders.
     */
    private final Map<UUID, ServerSenderStreamHolder<?,?>> activeServerSenderStreams;
    //private final Map<UUID, ClientSenderStreamHolder<?,?>> activeClientSenderStreams;    // Only Streams from server to client are supported
    //private final Map<UUID, ServerReceiverStreamHolder<?,?>> activeServerReceiverStreams; // Only Streams from server to client are supported
    private final Map<UUID, ClientReceiverStreamHolder<?,?>> activeClientReceiverStreams;

    /**
     * Lists to hold the stream IDs that need to be removed later. (after tick update)
     * This is used to avoid concurrent modification exceptions during tick updates.
     */
    private final List<UUID> toRemoveServerSenderLater = new java.util.ArrayList<>();
    //private final List<UUID> toRemoveClientSenderLater = new java.util.ArrayList<>(); // Only Streams from server to client are supported

    /**
     * Flags to check if the manager is currently in a tick update.
     * This is used to avoid concurrent modification exceptions during tick updates.
     */
    private boolean isInServerTickUpdate = false;
    //private boolean isInClientTickUpdate = false; // Only Streams from server to client are supported



    public StreamManager(@NotNull PacketManager networkManager) {
        if(networkManager == null)
        {
            throw new IllegalArgumentException("NetworkManager cannot be null. Please provide a valid NetworkManager instance.");
        }
        this.networkManager = networkManager;

        activeServerSenderStreams = new java.util.concurrent.ConcurrentHashMap<>();
        //activeClientSenderStreams = new java.util.concurrent.ConcurrentHashMap<>();   // Only Streams from server to client are supported
        //activeServerReceiverStreams = new java.util.concurrent.ConcurrentHashMap<>(); // Only Streams from server to client are supported
        activeClientReceiverStreams = new java.util.concurrent.ConcurrentHashMap<>();
        // TickEvent.PLAYER_POST.register(this::onClientTickUpdate); // Only Streams from server to client are supported
        TickEvent.SERVER_POST.register(this::onServerTickUpdate);
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
    public <CONTEXT_DATA, DATA> UUID startServerToClientStream(
            @NotNull GenericStream<CONTEXT_DATA, DATA> stream,
            @NotNull CONTEXT_DATA contextData,
            @NotNull Consumer<DATA> streamHandler,
            @Nullable Runnable streamStoppedHandler) {
        if(!isOnClient())
            throw new IllegalStateException("This method can only be called on the client side!");

        RegistryFriendlyByteBuf buf = UtilitiesPlatform.createRegistryFriendlyByteBufClientSide();
        stream.encodeContextData(buf, contextData);
        StreamStartPacket startPacket = new StreamStartPacket(stream.getStreamTypeID(), buf);
        UUID streamID = startPacket.getStreamID();
        ClientReceiverStreamHolder<CONTEXT_DATA, DATA> streamData = new ClientReceiverStreamHolder<>(networkManager, stream, streamHandler, streamStoppedHandler, streamID);
        activeClientReceiverStreams.put(streamID, streamData);
        networkManager.sendToServer(startPacket);
        return streamID;
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
    public <CONTEXT_DATA, DATA> UUID startClientToServerStream(
            @NotNull GenericStream<CONTEXT_DATA, DATA> stream,
            @NotNull CONTEXT_DATA contextData,
            @NotNull BiConsumer<DATA, ServerPlayer> streamHandler,
            @Nullable Runnable streamStoppedHandler,
            @NotNull ServerPlayer targetPlayer) {
        if(isOnClient())
            throw new IllegalStateException("This method can only be called on the server side!");

        RegistryFriendlyByteBuf buf = UtilitiesPlatform.createRegistryFriendlyByteBuf();
        stream.encodeContextData(buf, contextData);
        StreamStartPacket startPacket = new StreamStartPacket(stream.getStreamTypeID(), buf);
        UUID streamID = startPacket.getStreamID();
        ServerReceiverStreamHolder<CONTEXT_DATA, DATA> streamData = new ServerReceiverStreamHolder<>(networkManager, stream, streamHandler, streamStoppedHandler, streamID, targetPlayer.getUUID());
        activeServerReceiverStreams.put(streamID, streamData);
        networkManager.sendToClient(targetPlayer, startPacket);
        return streamID;
    }*/


    /**
     * Stops a stream with the given stream ID.
     * This method can be called on both the client and server side.
     * This method is safe to call from within the tick update of the stopping stream.
     */
    public void stopStream(@NotNull UUID streamID)
    {
        boolean isClient = isOnClient();
        boolean isServer = isOnServer();
        /*
        // Only Streams from server to client are supported
        if(activeClientSenderStreams != null && isClient) {
            if(isInClientTickUpdate) {
                toRemoveClientSenderLater.add(streamID);
            }
            else
            {
                var stream = activeClientSenderStreams.remove(streamID);
                if (stream != null) {
                    stream.streamStop();
                }
            }
        }*/
        if(activeServerSenderStreams != null && isServer) {
            if(isInServerTickUpdate)
            {
                toRemoveServerSenderLater.add(streamID);
            }
            else {
                var stream = activeServerSenderStreams.remove(streamID);
                if (stream != null) {
                    ServerPlayer targetPlayer = ServerPlayerUtilities.getOnlinePlayer(stream.playerUUID);
                    if (targetPlayer != null) {
                        stream.streamEnd();
                    }
                }
            }
        }

        if(activeClientReceiverStreams != null && isClient)
        {
            var stream = activeClientReceiverStreams.remove(streamID);
            if(stream != null)
            {
                stream.onStreamStopped();
            }
        }
        /*
        // Only Streams from server to client are supported
        if(activeServerReceiverStreams != null && isServer)
        {
            var stream = activeServerReceiverStreams.remove(streamID);
            if(stream != null)
            {
                stream.onStreamStopped(); // No player context on server side
            }
        }*/
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
        return activeServerSenderStreams.entrySet().stream()
                .map(entry -> new Tuple<>(entry.getKey(), entry.getValue().stream.getStreamTypeID()))
                .toList();
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
        return new ArrayList<>(); // Only Streams from server to client are supported
        /*return activeClientSenderStreams.entrySet().stream()
                .map(entry -> new Tuple<>(entry.getKey(), entry.getValue().stream.getStreamTypeID()))
                .toList();*/
    }

    /**
     * Gets a list of all active client->server streams.
     * The list holds tuples of
     *      - UUID: the unique stream ID
     *      - String: the stream type ID that can be used in the registry to get the stream object.
     * @return A list of UUIDs of the active server receiver streams.
     */
    public List<Tuple<UUID, String>> getActiveServerReceiverStreams()
    {
        return new ArrayList<>();
        /*return activeServerReceiverStreams.entrySet().stream()
                .map(entry -> new Tuple<>(entry.getKey(), entry.getValue().stream.getStreamTypeID()))
                .toList();*/
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
        return activeClientReceiverStreams.entrySet().stream()
                .map(entry -> new Tuple<>(entry.getKey(), entry.getValue().stream.getStreamTypeID()))
                .toList();
    }




    /**
     * Checks if the code is running on the client side.
     * @return true if the code is running on the client side, false otherwise.
     */
    public static boolean isOnClient()
    {
        return UtilitiesPlatform.codeCalledFromCliendSide();
    }

    /**
     * Checks if the code is running on the server side.
     * @return true if the code is running on the server side, false otherwise.
     */
    public static boolean isOnServer()
    {
        return UtilitiesPlatform.codeCalledFromServerSide();
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
     * This method is called on the server side to update the active server sender streams.
     * @param s unused server instance
     */
    public void onServerTickUpdate(MinecraftServer s)
    {
        isInServerTickUpdate = true;
        for(var entry : activeServerSenderStreams.entrySet())
        {
            UUID UUID = entry.getKey();
            ServerSenderStreamHolder<?, ?> streamData = entry.getValue();
            streamData.update();
            if(streamData.isDoRemove())
            {
                toRemoveServerSenderLater.add(UUID);
            }
        }
        if(!toRemoveServerSenderLater.isEmpty()) {
            for (UUID streamID : toRemoveServerSenderLater) {
                var stream = activeServerSenderStreams.remove(streamID);
                if(!stream.isDoRemove())
                    stream.streamEnd();
            }
            toRemoveServerSenderLater.clear();
        }
        isInServerTickUpdate = false;
    }


    /**
     * INTERNAL METHODE, DO NOT CALL THIS METHOD MANUALLY!
     *
     * This method is called on the client side to update the active client sender streams.
     * @param p unused player instance
     */
    public void onClientTickUpdate(Player p)
    {
        /*
        // Only Streams from server to client are supported
        isInClientTickUpdate = true;
        for(var entry : activeClientSenderStreams.entrySet())
        {
            UUID UUID = entry.getKey();
            ClientSenderStreamHolder<?, ?> streamData = entry.getValue();
            streamData.update();
            if(streamData.isDoRemove())
            {
                toRemoveClientSenderLater.add(UUID);
            }
        }
        if(!toRemoveClientSenderLater.isEmpty()) {
            for (UUID streamID : toRemoveClientSenderLater) {
                var stream = activeClientSenderStreams.remove(streamID);
                if(!stream.isDoRemove())
                    stream.streamStop();
            }
            toRemoveClientSenderLater.clear();
        }
        isInClientTickUpdate = false;
        */
    }


    /**
     * INTERNAL METHODE, DO NOT CALL THIS METHOD MANUALLY!
     *
     * Gets called when a stream-start-request has been received on the server side.
     * Starts a server to client stream.
     * @param stream The registered stream object used for the stream.
     * @param streamID The ID of the stream that is being started.
     * @param contextDataBuffer The context data buffer that contains the context data for the stream.
     * @param targetPlayer The player that the stream is being sent to.
     * @param <CONTEXT_DATA> The type of context data associated with the stream.
     * @param <DATA>         The type of data that the stream will handle.
     */
    public <CONTEXT_DATA, DATA> void startServerSenderStream(@NotNull GenericStream<CONTEXT_DATA, DATA> stream,
                                                             @NotNull UUID streamID,
                                                             @NotNull RegistryFriendlyByteBuf contextDataBuffer,
                                                             @NotNull ServerPlayer targetPlayer)
    {
        if(isOnClient())
            throw new IllegalStateException("This method can only be called on the server side!");
        ServerSenderStreamHolder<CONTEXT_DATA, DATA> streamData = new ServerSenderStreamHolder<>(networkManager, stream, contextDataBuffer, targetPlayer.getUUID(), streamID);
        if(activeServerSenderStreams.containsKey(streamID))
        {
            error("Stream ID conflict! Stream with ID "+streamID+" is already active!");
            return; // Stream ID already in use
        }
        activeServerSenderStreams.put(streamID, streamData);
        streamData.stream.onStartStreamSendingOnSever();
    }


    /**
     * INTERNAL METHODE, DO NOT CALL THIS METHOD MANUALLY!
     * Starts a client sender stream.
     * This method can only be called on the server side.
     * @param stream The registered stream object used for the stream.
     * @param streamID The ID of the stream that is being started.
     * @param contextDataBuffer The context data buffer that contains the context data for the stream.
     * @param <CONTEXT_DATA> The type of context data associated with the stream.
     * @param <DATA>         The type of data that the stream will handle.
     */
    public <CONTEXT_DATA, DATA> void startClientSenderStream(@NotNull GenericStream<CONTEXT_DATA, DATA> stream,
                                                             @NotNull UUID streamID,
                                                             @NotNull FriendlyByteBuf contextDataBuffer)
    {
        throw new IllegalStateException("Only Streams from server to client are supported");
        /*
        if(isOnClient())
            throw new IllegalStateException("This method can only be called on the client side!");
        ClientSenderStreamHolder<CONTEXT_DATA, DATA> streamData = new ClientSenderStreamHolder<>(networkManager, stream, contextDataBuffer, streamID);
        if(activeServerSenderStreams.containsKey(streamID))
        {
            error("Stream ID conflict! Stream with ID "+streamID+" is already active!");
            return; // Stream ID already in use
        }
        activeClientSenderStreams.put(streamID, streamData);
        streamData.stream.onStartStreamSendingOnClient();

        */
    }


    /**
     * INTERNAL METHODE, DO NOT CALL THIS METHOD MANUALLY!
     *
     * Sends a stream packet to the server or client, depending on the context.
     * This method is used to send the stream data to the other side.
     * @param streamID The ID of the stream that is being sent.
     */
    public void sendStreamPacket(@NotNull UUID streamID) {
        /*if(isOnClient()) {

            // Only Streams from server to client are supported

            if (activeClientSenderStreams != null) {
                ClientSenderStreamHolder<?, ?> streamData = activeClientSenderStreams.get(streamID);
                if (streamData != null) {
                    GenericStream<?, ?> stream = streamData.stream;
                    if (stream != null) {
                        RegistryFriendlyByteBuf buf = UtilitiesPlatform.createRegistryFriendlyByteBuf();
                        stream.createStreamPacketOnClient(buf);
                        networkManager.sendToServer(new GenericStreamPacket(streamID, buf));
                    }
                }
            }
        }*/
        if(isOnServer()) {
            if (activeServerSenderStreams != null) {
                ServerSenderStreamHolder<?, ?> streamData = activeServerSenderStreams.get(streamID);
                if (streamData != null) {
                    GenericStream<?, ?> stream = streamData.stream;
                    if (stream != null) {
                        RegistryFriendlyByteBuf buf = UtilitiesPlatform.createRegistryFriendlyByteBufServerSide();
                        stream.createStreamPacketOnServer(buf);
                        ServerPlayer targetPlayer = ServerPlayerUtilities.getOnlinePlayer(streamData.playerUUID);
                        if (targetPlayer == null) {
                            warn("Cannot send stream packet for stream ID "+streamID+" to player "+streamData.playerUUID+", player is not online!");
                            streamData.streamEnd();
                            return; // Player not online, cannot send stream packet
                        }
                        networkManager.sendToClient(targetPlayer, new GenericStreamPacket(streamID, buf));
                    }
                }
            }
        }
    }

    /**
     * INTERNAL METHODE, DO NOT CALL THIS METHOD MANUALLY!
     *
     * Handles the stream stop packet on the client side.
     * This method is called when a StreamStopPacket is received on the client side.
     * @param packet The StreamStopPacket that was received.
     */
    public void handlePacketOnClient(StreamStopPacket packet)
    {
        UUID streamID = packet.getStreamID();
        /*
        // Only Streams from server to client are supported
        if(activeClientSenderStreams != null) {
            ClientSenderStreamHolder<?, ?> streamData = activeClientSenderStreams.remove(streamID);
            if(streamData != null) {
                streamData.streamStop();
            }
        }*/
        if(activeClientReceiverStreams != null) {
            ClientReceiverStreamHolder<?, ?> streamData = activeClientReceiverStreams.remove(streamID);
            if(streamData != null) {
                streamData.onStreamStopped();
            }
        }
    }

    /**
     * INTERNAL METHODE, DO NOT CALL THIS METHOD MANUALLY!
     *
     * Handles the stream stop packet on the server side.
     * This method is called when a StreamStopPacket is received on the server side.
     * @param packet The StreamStopPacket that was received.
     */
    public void handlePacketOnServer(StreamStopPacket packet)
    {
        UUID streamID = packet.getStreamID();
       if(activeServerSenderStreams != null) {
            ServerSenderStreamHolder<?, ?> streamData = activeServerSenderStreams.remove(streamID);
            if(streamData != null) {
                streamData.streamEnd();
            }
        }
       /*
       // Only Streams from server to client are supported
        if(activeServerReceiverStreams != null) {
            ServerReceiverStreamHolder<?, ?> streamData = activeServerReceiverStreams.remove(streamID);
            if(streamData != null) {
                streamData.onStreamStopped(); // No player context on server side
            }
        }*/
    }


    /**
     * INTERNAL METHODE, DO NOT CALL THIS METHOD MANUALLY!
     *
     * Handles a generic stream packet on the client side.
     * @param packet The GenericStreamPacket that was received.
     */
    public void handlePacketOnClient(GenericStreamPacket packet)
    {
        UUID streamID = packet.getStreamID();
        ClientReceiverStreamHolder<?, ?> streamData = activeClientReceiverStreams.get(streamID);
        if(streamData != null) {
            streamData.handleStreamPacket(packet.getData());
        }
    }

    /**
     * INTERNAL METHODE, DO NOT CALL THIS METHOD MANUALLY!
     *
     * Handles a generic stream packet on the server side.
     * @param packet The GenericStreamPacket that was received.
     * @param sender The player that sent the packet.
     */
    public void handlePacketOnServer(GenericStreamPacket packet, ServerPlayer sender)
    {
        /*
        // Only Streams from server to client are supported
        UUID streamID = packet.getStreamID();
        ServerReceiverStreamHolder<?, ?> streamData = activeServerReceiverStreams.get(streamID);
        if(streamData != null) {
            streamData.handleStreamPacket(packet.getData(), sender);
        }*/
    }


    private void error(String msg, Throwable e) {
        ModUtilitiesMod.LOGGER.error("[StreamManager] " + msg, e);
    }
    private void error(String msg) {
        ModUtilitiesMod.LOGGER.error("[StreamManager] " + msg);
    }
    private void warn(String msg) {
        ModUtilitiesMod.LOGGER.warn("[StreamManager] " + msg);
    }
}
