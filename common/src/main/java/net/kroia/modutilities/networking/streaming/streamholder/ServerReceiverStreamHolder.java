package net.kroia.modutilities.networking.streaming.streamholder;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.ServerPlayerUtilities;
import net.kroia.modutilities.networking.PacketManager;
import net.kroia.modutilities.networking.streaming.GenericStream;
import net.kroia.modutilities.networking.streaming.StreamStopPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * This class is used to hold the data of a server->client stream, located on the server side.
 * @param <CONTEXT_DATA> The type of context data associated with the stream.
 * @param <DATA>         The type of data that the stream will handle.
 */
public class ServerReceiverStreamHolder<CONTEXT_DATA, DATA>
{
    /**
     * The NetworkManager instance used to send packets to the target player.
     */
    private final PacketManager networkManager;

    /**
     * Not a copy of the registered stream object, but the actual registered stream object.
     * It is only used to decode the data from the stream packet.
     */
    public final GenericStream<CONTEXT_DATA, DATA> stream;

    /**
     * The handler that will be called when a stream packet is received.
     * It is a BiConsumer that takes the decoded data and the player from which the stream is coming as arguments.
     */
    public final BiConsumer<DATA, ServerPlayer> streamHandler;

    /**
     * The handler that will be called when the stream is stopped.
     * It is a Runnable that will be executed when the stream is stopped.
     */
    public final Runnable streamStoppedHandler;

    /**
     * The UUID of the stream, used to identify the stream.
     */
    public final UUID streamID;

    /**
     * The UUID of the player that sent the stream.
     * This is used to send a stop packet back to the player when the stream is stopped.
     */
    public final UUID playerSenderUUID;

    /**
     * Flag to check if the stream is stopped.
     * This is used to prevent multiple calls to the stream stopped handler.
     */
    private boolean isStpped = false; // Flag to check if the stream is stopped

    public ServerReceiverStreamHolder(PacketManager networkManager,
                                      GenericStream<CONTEXT_DATA, DATA> stream,
                                      BiConsumer<DATA, ServerPlayer> streamHandler,
                                      Runnable streamStoppedHandler,
                                      UUID streamID,
                                      UUID playerSenderUUID) {
        this.networkManager = networkManager;
        this.stream = stream;
        this.streamHandler = streamHandler;
        this.streamStoppedHandler = streamStoppedHandler;
        this.streamID = streamID;
        this.playerSenderUUID = playerSenderUUID;
    }

    /**
     * Handles the stream packet received from the client.
     * It decodes the data from the packet and calls the stream handler with the decoded data and the player.
     *
     * @param buf   The FriendlyByteBuf containing the packet data.
     * @param player The player that sent the packet.
     */
    public void handleStreamPacket(RegistryFriendlyByteBuf buf, ServerPlayer player) {
        if (streamHandler != null) {
            DATA data = stream.decodeData(buf);
            try{
                streamHandler.accept(data, player);
            } catch (Exception e) {
                error("Error while calling stream packet handler for: "+stream, e);
            }
        }
    }

    /**
     * Called when the stream is stopped.
     * It calls the stream stopped handler and sends a StreamStopPacket to the target player.
     */
    public void onStreamStopped() {
        if (streamStoppedHandler != null && !isStpped) {
            isStpped = true; // Mark as stopped
            try {
                streamStoppedHandler.run(); // Call the stream stopped handler
            }
            catch (Exception e) {
                error("Error while calling stream stop handler for: "+stream, e);
            }
            ServerPlayer targetPlayer = ServerPlayerUtilities.getOnlinePlayer(playerSenderUUID);
            if (targetPlayer != null) {
                StreamStopPacket stopPacket = new StreamStopPacket(streamID);
                networkManager.sendToClient(targetPlayer, stopPacket);
            }
        }
    }
    private void error(String msg, Throwable e) {
        ModUtilitiesMod.LOGGER.error("[ServerReceiverStreamHolder] " + msg, e);
    }
}