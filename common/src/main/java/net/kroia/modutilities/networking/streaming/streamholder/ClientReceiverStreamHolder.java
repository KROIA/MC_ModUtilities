package net.kroia.modutilities.networking.streaming.streamholder;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.PacketManager;
import net.kroia.modutilities.networking.streaming.GenericStream;
import net.kroia.modutilities.networking.streaming.StreamStopPacket;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * This class is used to hold the data of a server->client stream, located on the client side.
 * @param <CONTEXT_DATA> The type of context data associated with the stream.
 * @param <DATA>         The type of data that the stream will handle.
 */
public class ClientReceiverStreamHolder<CONTEXT_DATA, DATA>
{
    /**
     * The NetworkManager instance used to send packets to the server.
     */
    private final PacketManager networkManager;

    /**
     * Not a copy of the registered stream object, but the actual registered stream object.
     * It is only used to decode the data from the stream packet.
     */
    public final GenericStream<CONTEXT_DATA, DATA> stream;

    /**
     * The handler that will be called when a stream packet is received.
     * It is a Consumer that takes the decoded data as an argument.
     */
    public final Consumer<DATA> streamHandler;

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
     * Flag to check if the stream is stopped.
     * This is used to prevent multiple calls to the stream stopped handler.
     */
    private boolean isStpped = false; // Flag to check if the stream is stopped
    public ClientReceiverStreamHolder(PacketManager networkManager,
                                      GenericStream<CONTEXT_DATA, DATA> stream,
                                      Consumer<DATA> streamHandler,
                                      Runnable streamStoppedHandler,
                                      UUID streamID) {
        this.networkManager = networkManager;
        this.stream = stream;
        this.streamHandler = streamHandler;
        this.streamStoppedHandler = streamStoppedHandler;
        this.streamID = streamID;
    }


    /**
     * Handles the stream packet received from the server.
     * It decodes the data from the packet and calls the stream handler with the decoded data.
     *
     * @param buf The FriendlyByteBuf containing the packet data.
     */
    public void handleStreamPacket(FriendlyByteBuf buf) {
        if (streamHandler != null) {
            DATA data = stream.decodeData(buf);
            try {
                streamHandler.accept(data);
            }
            catch (Exception e) {
                error("Error while calling stream packet handler for: " + stream, e);
            }
        }
    }

    /**
     * Stops the stream and sends a stop packet to the server for notification.
     * It also calls the stream stopped handler if it is set.
     */
    public void onStreamStopped() {
        if (streamStoppedHandler != null && !isStpped) {
            isStpped = true; // Mark as stopped
            try {
                streamStoppedHandler.run(); // Call the stream stopped handler
            }
            catch (Exception e) {
                error("Error while calling stream stop handler for: " + stream, e);
            }


            StreamStopPacket stopPacket = new StreamStopPacket(streamID);
            networkManager.sendToServer(stopPacket);
        }
    }
    private void error(String msg, Throwable e) {
        ModUtilitiesMod.LOGGER.error("[ClientReceiverStreamHolder] " + msg, e);
    }
}