package net.kroia.modutilities.networking.client_server.arrs;


import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.UtilitiesPlatform;
import net.kroia.modutilities.networking.NetworkPacketManager;
import net.kroia.modutilities.networking.client_server.arrs.requestholder.ClientRequestHolder;
import net.kroia.modutilities.networking.client_server.arrs.requestholder.ServerRequestHolder;
import net.kroia.modutilities.networking.server_server.ForwardPacketContext;
import net.kroia.modutilities.networking.server_server.ServerServerManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The RequestManager class executes and keeps track of requests sent between the client and server.
 * A request will stay in a list until the response is received.
 *
 * This class needs access to the NetworkManager from this utility mod to send requests.
 *
 */
public class RequestManager {



    private final NetworkPacketManager networkManager;
    private final Map<UUID, ServerRequestHolder<?,?>> pendingServerRequests = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, ServerRequestHolder<?,?>> pendingServerServerRequests = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, ClientRequestHolder<?,?>> pendingClientRequests = new java.util.concurrent.ConcurrentHashMap<>();


    /**
     * Constructs a RequestManager with the specified NetworkManager.
     * The NetworkManager is used to send requests to the server or client.
     *
     * @param networkManager The NetworkManager to use for sending requests.
     */
    public RequestManager(@NotNull NetworkPacketManager networkManager) {
        if(networkManager == null) {
            throw new IllegalArgumentException("NetworkManager cannot be null. Please provide a valid NetworkManager instance.");
        }
        this.networkManager = networkManager;
    }

    /**
     * Gets the NetworkManager used by this RequestManager.
     */
    public NetworkPacketManager getNetworkManager() {
        return networkManager;
    }


    /**
     * Sends a request to the server and registers a response handler.
     * The request will be sent using the NetworkManager set in this RequestManager.
     *
     * @param request The request to send.
     * @param input The input data for the request delivered to the receiver.
     * @param <IN> The type of input data.
     * @param <OUT> The type of output data provided by the provider
     */
    public <IN, OUT> CompletableFuture<OUT> sendRequestToServer(@NotNull GenericRequest<IN, OUT> request,
                                                                IN input) {
        RegistryFriendlyByteBuf buf = UtilitiesPlatform.createRegistryFriendlyByteBufClientSide();
        request.encodeInput(buf, input);
        GenericRequestPacket requestPacket = new GenericRequestPacket(request.getRequestTypeID(), buf);
        ServerRequestHolder<IN, OUT> requestData = new ServerRequestHolder<>();
        requestData.responseFuture = new CompletableFuture<>();
        requestData.requestPacket = requestPacket;
        requestData.request = request;
        UUID requestId = requestPacket.getRequestID();

        pendingServerRequests.put(requestId, requestData);
        networkManager.sendToServer(requestPacket);
        return requestData.responseFuture;
    }

    /**
     * Sends a request to the master server and registers a response handler.
     * The request will be sent using the NetworkManager set in this RequestManager.
     *
     * @param request The request to send.
     * @param input The input data for the request delivered to the receiver.
     * @param <IN> The type of input data.
     * @param <OUT> The type of output data provided by the provider
     */
    public <IN, OUT> CompletableFuture<OUT> sendRequestToMaster(@NotNull GenericRequest<IN, OUT> request,
                                                                IN input) {
        RegistryFriendlyByteBuf buf = UtilitiesPlatform.createRegistryFriendlyByteBufServerSide();
        request.encodeInput(buf, input);
        GenericRequestPacket requestPacket = new GenericRequestPacket(request.getRequestTypeID(), buf);
        ServerRequestHolder<IN, OUT> requestData = new ServerRequestHolder<>();
        requestData.responseFuture = new CompletableFuture<>();
        requestData.requestPacket = requestPacket;
        requestData.request = request;
        UUID requestId = requestPacket.getRequestID();

        pendingServerServerRequests.put(requestId, requestData);
        ServerServerManager.sendToMaster(requestPacket);
        return requestData.responseFuture;
    }

    /**
     * Sends a request to a specific client and registers a response handler.
     * The request will be sent using the NetworkManager set in this RequestManager.
     *
     * @param request The request to send.
     * @param input The input data for the request delivered to the receiver.
     * @param target The target player to send the request to.
     * @param responseHandler The handler to call when the response is received.
     * @param <IN> The type of input data.
     * @param <OUT> The type of output data provided by the provider
     */
    /*public <IN, OUT> void sendRequestToClient(@NotNull GenericRequest<IN, OUT> request,
                                              IN input,
                                              @NotNull ServerPlayer target,
                                              @NotNull BiConsumer<OUT, ServerPlayer> responseHandler) {

        RegistryFriendlyByteBuf buf = UtilitiesPlatform.createRegistryFriendlyByteBufServerSide();
        request.encodeInput(buf, input);
        GenericRequestPacket requestPacket = new GenericRequestPacket(request.getRequestTypeID(), buf);
        ClientRequestHolder<IN, OUT> requestData = new ClientRequestHolder<>();
        requestData.responseHandler = responseHandler;
        requestData.requestPacket = requestPacket;
        requestData.request = request;
        UUID requestId = requestPacket.getRequestID();

        pendingClientRequests.put(requestId, requestData);
        networkManager.sendToClient(target, requestPacket);
    }*/

    /**
     * Gets the number of requests that have been sent to the server but have not yet received a response.
     * @return The number of pending server requests.
     */
    public int getPendingServerRequestsCount() {
        return pendingServerRequests.size();
    }

    /**
     * Gets the number of requests that have been sent to the client but have not yet received a response.
     * @return The number of pending client requests.
     */
    public int getPendingClientRequestsCount() {
        return pendingClientRequests.size();
    }

    /**
     * Clears all pending server requests.
     */
    public void clearPendingServerRequests() {
        pendingServerRequests.clear();
    }

    /**
     * Clears all pending client requests.
     */
    public void clearPendingClientRequests() {
        pendingClientRequests.clear();
    }

    /**
     * Clears all pending requests, both server and client.
     */
    public void clearPendingRequests() {
        clearPendingServerRequests();
        clearPendingClientRequests();
    }


    /**
     * Processes a response packet received on the client side.
     * This method will look up the request data using the request ID and call the response handler.
     * This function is called automatically when a GenericResponsePacket is received on the client side.
     *
     * @param responsePacket The response packet received from the server.
     */
    public void processResponseOnClient(GenericResponsePacket responsePacket)
    {
        UUID requestId = responsePacket.getRequestID();
        ServerRequestHolder<?, ?> requestData = pendingServerRequests.remove(requestId);
        if(requestData == null)
            return;

        requestData.processResponse(responsePacket.getData());
    }

    public void processResponseOnSlave(GenericResponsePacket responsePacket, ForwardPacketContext context)
    {
        UUID requestId = responsePacket.getRequestID();
        ServerRequestHolder<?, ?> requestData = pendingServerServerRequests.remove(requestId);
        if(requestData == null)
        {
            // Forward the response to the client
            var request = AsynchronousRequestResponseSystem.getRegisteredRequest(responsePacket.getRequestTypeID());
            if (request == null) {
                return; // No factory found for this request type
            }
            //ModUtilitiesMod.LOGGER.info("Handle response on slave server: "+requestTypeID);
            //RegistryFriendlyByteBuf responseData = UtilitiesPlatform.createRegistryFriendlyByteBufServerSide();
            try {
                MinecraftServer server = UtilitiesPlatform.getServer();
                if(server == null)
                    return;
                ServerPlayer targetPlayer = server.getPlayerList().getPlayer(context.senderPlayerUUID);
                if(targetPlayer == null)
                    return;
                request.getManager().getNetworkManager().sendToClient(targetPlayer, responsePacket);
            }
            catch (Exception e) {
                // Handle any exceptions that may occur during decoding/encoding
                ModUtilitiesMod.LOGGER.error("Error handling GenericResponsePacket: " + e.getMessage(), e);
                return; // Exit if an error occurs
            }
        }
        else
            requestData.processResponse(responsePacket.getData());
    }

    /**
     * Processes a response packet received on the server side.
     * This method will look up the request data using the request ID and call the response handler.
     * This function is called automatically when a GenericResponsePacket is received on the server side.
     *
     * @param responsePacket The response packet received from the client.
     * @param player The player who sent the request.
     */
    public void processResponseOnServer(GenericResponsePacket responsePacket, ServerPlayer player) {
        UUID requestId = responsePacket.getRequestID();
        ClientRequestHolder<?, ?> requestData = pendingClientRequests.remove(requestId);
        if(requestData == null)
            return;

        requestData.processResponse(responsePacket.getData(), player);
    }
}
