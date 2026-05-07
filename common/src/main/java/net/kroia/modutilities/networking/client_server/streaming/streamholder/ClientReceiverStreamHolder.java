package net.kroia.modutilities.networking.client_server.streaming.streamholder;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.NetworkPacketManager;
import net.kroia.modutilities.networking.client_server.streaming.GenericStream;
import net.kroia.modutilities.networking.client_server.streaming.StreamStopClientSenderPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;

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
    private final NetworkPacketManager networkManager;

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
    private boolean isStopped = false; // Flag to check if the stream is stopped

    /**
     * Creates a new ClientReceiverStreamHolder tracking the client-side state of a server-to-client stream.
     *
     * @param networkManager       The NetworkPacketManager used to send the stop echo back to the server.
     * @param stream               The registered stream definition (used for decoding incoming data).
     * @param streamHandler        The consumer invoked for each decoded data chunk received.
     * @param streamStoppedHandler Optional runnable invoked once the stream has stopped.
     * @param streamID             The unique stream UUID identifying this stream instance.
     */
    public ClientReceiverStreamHolder(NetworkPacketManager networkManager,
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
    public void handleStreamPacket(RegistryFriendlyByteBuf buf) {
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
     * Marks this stream as stopped, invokes the stream stopped handler (once),
     * and optionally sends a stop notification back to the server so it can clean up
     * its sender side.
     *
     * @param sendEcho When true, a {@link StreamStopClientSenderPacket} is sent back to the server.
     */
    public void onStreamStopped(boolean sendEcho) {
        if (isStopped) return;
        isStopped = true; // Mark as stopped

        if (streamStoppedHandler != null) {
            try {
                streamStoppedHandler.run(); // Call the stream stopped handler
            }
            catch (Exception e) {
                error("Error while calling stream stop handler for: " + stream, e);
            }
        }

        if(sendEcho)
        {
            StreamStopClientSenderPacket stopPacket = new StreamStopClientSenderPacket(streamID);
            networkManager.sendToServer(stopPacket);
        }
    }
    private void error(String msg, Throwable e) {
        ModUtilitiesMod.LOGGER.error("[ClientReceiverStreamHolder] " + msg, e);
    }
}