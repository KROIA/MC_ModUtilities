package net.kroia.modutilities.networking.client_server.arrs;


import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.UtilitiesPlatform;
import net.kroia.modutilities.networking.NetworkPacketManager;
import net.kroia.modutilities.networking.client_server.arrs.requestholder.ClientRequestHolder;
import net.kroia.modutilities.networking.client_server.arrs.requestholder.ServerRequestHolder;
import net.kroia.modutilities.networking.multi_server.ForwardPacketContext;
import net.kroia.modutilities.networking.multi_server.MultiServerManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The RequestManager executes and tracks requests sent between client and server (and, in
 * a multi-server topology, between master and slave servers). Pending requests are stored
 * in three thread-safe maps keyed by request UUID:
 * <ul>
 *     <li>{@code pendingServerRequests} — requests sent client to server</li>
 *     <li>{@code pendingServerServerRequests} — requests sent between master and slave servers</li>
 *     <li>{@code pendingClientRequests} — requests sent server to client (currently unused)</li>
 * </ul>
 * This class needs access to the {@link NetworkPacketManager} from this utility mod to send requests.
 *
 * @apiNote Callers should periodically invoke {@link #cleanupExpiredRequests(long)} to free
 *          pending requests whose responses never arrive (otherwise the maps would grow without bound).
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
     * @return The {@link NetworkPacketManager} used by this RequestManager to send packets.
     */
    public NetworkPacketManager getNetworkManager() {
        return networkManager;
    }


    /**
     * Sends a request to the server and tracks the pending response.
     * The request is encoded using the configured {@link NetworkPacketManager} (client-side buffer).
     *
     * @param request The request to send.
     * @param input   The input data for the request delivered to the receiver.
     * @param <IN>    The type of input data.
     * @param <OUT>   The type of output data provided by the responder.
     * @return A future that completes with the response data, or with a completed-null future if
     *         the client cannot allocate a buffer (e.g. not in an active world).
     */
    public <IN, OUT> CompletableFuture<OUT> sendRequestToServer(@NotNull GenericRequest<IN, OUT> request,
                                                                IN input) {
        RegistryFriendlyByteBuf buf = UtilitiesPlatform.createRegistryFriendlyByteBufClientSide();
        if(buf == null)
        {
            error("sendRequestToServer(request, input): Can't create byte buf to encode the input data. Is the client in a active world?");
            return CompletableFuture.completedFuture(null);
        }
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
     * Sends a request from a slave server to the master server and tracks the pending response.
     *
     * @param request The request to send.
     * @param input   The input data for the request delivered to the receiver.
     * @param <IN>    The type of input data.
     * @param <OUT>   The type of output data provided by the responder.
     * @return A future that completes with the response data, or with a completed-null future if
     *         a server-side buffer cannot be allocated.
     * @apiNote Intended to be called on the slave server only.
     */
    public <IN, OUT> CompletableFuture<OUT> sendRequestToMaster(@NotNull GenericRequest<IN, OUT> request,
                                                                IN input) {
        RegistryFriendlyByteBuf buf = UtilitiesPlatform.createRegistryFriendlyByteBufServerSide();
        if(buf == null)
        {
            error("GenericResponsePacket(request, input): Can't create byte buf to encode the input data. Is the server running?");
            return CompletableFuture.completedFuture(null);
        }
        request.encodeInput(buf, input);
        GenericRequestPacket requestPacket = new GenericRequestPacket(request.getRequestTypeID(), buf);
        ServerRequestHolder<IN, OUT> requestData = new ServerRequestHolder<>();
        requestData.responseFuture = new CompletableFuture<>();
        requestData.requestPacket = requestPacket;
        requestData.request = request;
        UUID requestId = requestPacket.getRequestID();

        pendingServerServerRequests.put(requestId, requestData);
        MultiServerManager.sendToMaster(requestPacket);
        return requestData.responseFuture;
    }
    /**
     * Sends a request from the master server to a specific slave server and tracks the pending response.
     *
     * @param request The request to send.
     * @param slaveID The identifier of the target slave server.
     * @param input   The input data for the request delivered to the receiver.
     * @param <IN>    The type of input data.
     * @param <OUT>   The type of output data provided by the responder.
     * @return A future that completes with the response data, or with a completed-null future if
     *         a server-side buffer cannot be allocated.
     * @apiNote Intended to be called on the master server only.
     */
    public <IN, OUT> CompletableFuture<OUT> sendRequestToSlave(@NotNull GenericRequest<IN, OUT> request,
                                                               String slaveID,
                                                               IN input) {
        RegistryFriendlyByteBuf buf = UtilitiesPlatform.createRegistryFriendlyByteBufServerSide();
        if(buf == null)
        {
            error("sendRequestToSlave(request, slaveID, input): Can't create byte buf to encode the input data. Is the server running?");
            return CompletableFuture.completedFuture(null);
        }
        request.encodeInput(buf, input);
        GenericRequestPacket requestPacket = new GenericRequestPacket(request.getRequestTypeID(), buf);
        ServerRequestHolder<IN, OUT> requestData = new ServerRequestHolder<>();
        requestData.responseFuture = new CompletableFuture<>();
        requestData.requestPacket = requestPacket;
        requestData.request = request;
        UUID requestId = requestPacket.getRequestID();

        pendingServerServerRequests.put(requestId, requestData);
        MultiServerManager.sendToSlave(slaveID, requestPacket);
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
     * Default timeout for pending requests in milliseconds (30 seconds).
     */
    public static final long DEFAULT_TIMEOUT_MS = 30_000L;

    /**
     * Removes pending requests older than the given age. Pending server requests have their
     * response futures completed exceptionally with a {@link java.util.concurrent.TimeoutException}.
     * Callers should invoke this periodically (e.g. on a server tick) to prevent unbounded
     * memory growth when responses never arrive.
     *
     * @param maxAgeMs maximum age of a pending request in milliseconds
     * @return total number of expired requests that were removed
     */
    public int cleanupExpiredRequests(long maxAgeMs) {
        long cutoff = System.currentTimeMillis() - maxAgeMs;
        int removed = 0;
        var serverIt = pendingServerRequests.entrySet().iterator();
        while (serverIt.hasNext()) {
            var entry = serverIt.next();
            if (entry.getValue().creationTimeMs < cutoff) {
                if (entry.getValue().responseFuture != null && !entry.getValue().responseFuture.isDone()) {
                    entry.getValue().responseFuture.completeExceptionally(
                        new java.util.concurrent.TimeoutException("Request timed out: " + entry.getKey()));
                }
                serverIt.remove();
                removed++;
            }
        }
        var serverServerIt = pendingServerServerRequests.entrySet().iterator();
        while (serverServerIt.hasNext()) {
            var entry = serverServerIt.next();
            if (entry.getValue().creationTimeMs < cutoff) {
                if (entry.getValue().responseFuture != null && !entry.getValue().responseFuture.isDone()) {
                    entry.getValue().responseFuture.completeExceptionally(
                        new java.util.concurrent.TimeoutException("Request timed out: " + entry.getKey()));
                }
                serverServerIt.remove();
                removed++;
            }
        }
        var clientIt = pendingClientRequests.entrySet().iterator();
        while (clientIt.hasNext()) {
            var entry = clientIt.next();
            if (entry.getValue().creationTimeMs < cutoff) {
                clientIt.remove();
                removed++;
            }
        }
        return removed;
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


    /**
     * Processes a response packet received on the master server.
     * If the response matches a pending master-originated request, its future is completed.
     * Otherwise, the response is forwarded to the originating client (resolved via
     * {@code context.senderPlayerUUID}).
     *
     * @param responsePacket The response packet.
     * @param context        The forwarding context with origin sender information.
     */
    public void processResponseOnMaster(GenericResponsePacket responsePacket, ForwardPacketContext context)
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
                error("Error handling GenericResponsePacket: " + e.getMessage(), e);
                return; // Exit if an error occurs
            }
        }
        else
            requestData.processResponse(responsePacket.getData());
    }
    /**
     * Processes a response packet received on a slave server.
     * If the response matches a pending slave-originated request, its future is completed.
     * Otherwise, the response is forwarded to the originating client (resolved via
     * {@code context.senderPlayerUUID}).
     *
     * @param responsePacket The response packet.
     * @param context        The forwarding context with origin sender information.
     */
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
                error("Error handling GenericResponsePacket: " + e.getMessage(), e);
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

    protected void error(String msg)
    {
        ModUtilitiesMod.LOGGER.error("[RequestManager] " + msg);
    }
    protected void error(String msg, Throwable e)
    {
        ModUtilitiesMod.LOGGER.error("[RequestManager] " + msg, e);
    }
}
