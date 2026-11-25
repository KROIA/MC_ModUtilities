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
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

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
 * @apiNote While {@link #ENABLE_REQUEST_FAILURES} is on, ModUtilities calls {@link #cleanupExpiredRequests(boolean)}
 *          every 20 ticks and fails requests whose responses never arrive.
 */
public class RequestManager {

    /**
     * Switch for the request failure paths, off by default: timeouts (every 20 server and client ticks),
     * immediate failure when a master/slave send is refused, and the disconnect sweep. While off,
     * such requests stay pending forever (old behaviour).
     * <p>
     * Turn it on with {@code RequestManager.ENABLE_REQUEST_FAILURES = true;} once every caller that moves
     * value on failure tells a never-sent failure from a {@link RequestOutcomeUnknownException}
     * (BankSystem B6). The switch is JVM-wide and covers every mod using the ARRS.
     */
    public static volatile boolean ENABLE_REQUEST_FAILURES = false;



    private final NetworkPacketManager networkManager;
    private final Map<UUID, ServerRequestHolder<?,?>> pendingServerRequests = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, ServerRequestHolder<?,?>> pendingServerServerRequests = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, ClientRequestHolder<?,?>> pendingClientRequests = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, ForwardedRequest> forwardedClientRequests = new java.util.concurrent.ConcurrentHashMap<>();


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
     * @return See {@link GenericRequest#sendRequestToServer(Object)}: completes with the response data, or with
     *         {@code null} if the client cannot allocate a buffer (e.g. not in an active world; nothing was sent).
     *         While {@link #ENABLE_REQUEST_FAILURES} is on it fails with {@link RequestOutcomeUnknownException}
     *         when no response arrives within {@link GenericRequest#getTimeoutMs()} or a forwarding slave loses its
     *         master link, and with {@link RequestNotSentException} when a slave could not forward it to the master.
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
     * @return See {@link GenericRequest#sendRequestToMaster(Object)}.
     * @apiNote Intended to be called on the slave server only.
     */
    public <IN, OUT> CompletableFuture<OUT> sendRequestToMaster(@NotNull GenericRequest<IN, OUT> request,
                                                                IN input) {
        return sendRequestToMaster(request, null, input);
    }

    /**
     * Sends a request from a slave server to the master server on behalf of a player and tracks the pending response.
     *
     * @param request    The request to send.
     * @param playerUUID The player the master's {@link GenericRequest#handleOnMasterServer} receives as
     *                   {@code playerSender}, or {@code null} for none.
     * @param input      The input data for the request delivered to the receiver.
     * @param <IN>       The type of input data.
     * @param <OUT>      The type of output data provided by the responder.
     * @return See {@link GenericRequest#sendRequestToMaster(UUID, Object)}.
     * @apiNote Intended to be called on the slave server only. The response completes the returned future here;
     *          it is not relayed to the player.
     */
    public <IN, OUT> CompletableFuture<OUT> sendRequestToMaster(@NotNull GenericRequest<IN, OUT> request,
                                                                @Nullable UUID playerUUID,
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
        if(!sendToMaster(playerUUID, requestPacket))
            failNotSent(requestId, requestData);
        return requestData.responseFuture;
    }

    /**
     * Slave only: forwards a client's request to the master. While {@link #ENABLE_REQUEST_FAILURES} is on,
     * a refused send is answered to the client at once ({@link RequestNotSentException} there), and a sent
     * request is tracked until the master's response is relayed, so a lost master link fails it on the
     * client right away ({@link RequestOutcomeUnknownException}, see {@link #failRequestsOnConnectionLost}).
     * Call on the server thread.
     *
     * @param request    the registered request type of {@code packet}
     * @param packet     the client's request packet
     * @param playerUUID the client that sent it
     */
    public void forwardRequestToMaster(GenericRequest<?, ?> request, GenericRequestPacket packet, UUID playerUUID) {
        boolean sent;
        try {
            sent = sendToMaster(playerUUID, packet);
        } catch (RuntimeException e) {
            error("forwardRequestToMaster: " + e.getMessage(), e);
            sent = false;
        }
        if(!ENABLE_REQUEST_FAILURES)
            return; // old behaviour: the client waits
        if(sent)
            forwardedClientRequests.put(packet.getRequestID(),
                    new ForwardedRequest(playerUUID, packet.getRequestTypeID(), request.getTimeoutMs(), System.currentTimeMillis()));
        else
            sendFailureToClient(playerUUID, packet.getRequestID(), packet.getRequestTypeID(), GenericResponsePacket.STATUS_NOT_SENT);
    }

    /** A client request this slave forwarded to the master, awaiting the master's response. */
    private record ForwardedRequest(UUID playerUUID, String requestTypeID, long timeoutMs, long creationTimeMs) {}

    private void sendFailureToClient(UUID playerUUID, UUID requestID, String requestTypeID, byte status) {
        RegistryFriendlyByteBuf empty = UtilitiesPlatform.createRegistryFriendlyByteBufServerSide();
        if(empty == null)
            return; // server gone, so is the client
        sendToClient(playerUUID, new GenericResponsePacket(requestID, requestTypeID, empty, status));
    }

    /** Sends a response to a player on this server, if online; protected so tests can capture it. */
    protected void sendToClient(UUID playerUUID, GenericResponsePacket packet) {
        MinecraftServer server = UtilitiesPlatform.getServer();
        ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(playerUUID);
        if(player != null)
            networkManager.sendToClient(player, packet);
    }
    /**
     * Sends a request from the master server to a specific slave server and tracks the pending response.
     *
     * @param request The request to send.
     * @param slaveID The identifier of the target slave server.
     * @param input   The input data for the request delivered to the receiver.
     * @param <IN>    The type of input data.
     * @param <OUT>   The type of output data provided by the responder.
     * @return See {@link GenericRequest#sendRequestToSlave(String, Object)}.
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
        requestData.targetSlaveID = slaveID;
        UUID requestId = requestPacket.getRequestID();

        pendingServerServerRequests.put(requestId, requestData);
        if(!sendToSlave(slaveID, requestPacket))
            failNotSent(requestId, requestData);
        return requestData.responseFuture;
    }

    /** Sends to the master; protected so tests can simulate the link. @return false if the send was refused */
    protected boolean sendToMaster(@Nullable UUID senderPlayerUUID, GenericRequestPacket packet) {
        // Dev builds only: testing.SimulatedMasterHang (master hangs) drops the request but reports it sent while the
        // link is up, so it times out. Release code cannot reference testing/**, hence the property name.
        if(ModUtilitiesMod.ENABLE_DEV_FEATURES && Boolean.getBoolean("modutilities.dev.simulatedMasterHang"))
            return MultiServerManager.isRunning();
        return MultiServerManager.sendToMaster(senderPlayerUUID, packet);
    }

    /** Sends to a slave; protected so tests can simulate the link. @return false if the send was refused */
    protected boolean sendToSlave(String slaveID, GenericRequestPacket packet) {
        return MultiServerManager.sendToSlave(slaveID, packet);
    }

    private void failNotSent(UUID requestId, ServerRequestHolder<?, ?> holder) {
        if(!ENABLE_REQUEST_FAILURES)
            return; // old behaviour: stays pending
        pendingServerServerRequests.remove(requestId);
        holder.responseFuture.completeExceptionally(new RequestNotSentException(
                "ARRS request not sent: " + holder.request.getRequestTypeID()));
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

    /** Number of pending server-to-server requests (on a slave: requests to the master awaiting a response). */
    public int getPendingServerServerRequestsCount() {
        return pendingServerServerRequests.size();
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
     * Fails pending requests older than their {@link GenericRequest#getTimeoutMs()} with a
     * {@link RequestOutcomeUnknownException}. Runs on the thread that completes the futures of that side.
     *
     * @param clientSide {@code true}: requests this client sent to the server (client thread);
     *                   {@code false}: master/slave requests (server thread)
     * @return number of requests that timed out
     */
    public int cleanupExpiredRequests(boolean clientSide) {
        long now = System.currentTimeMillis();
        if(clientSide)
            return failPending(pendingServerRequests, h -> now - h.creationTimeMs > h.request.getTimeoutMs(), "timed out");
        // Server-to-client requests are unused and have no future: just drop old ones.
        pendingClientRequests.values().removeIf(h -> now - h.creationTimeMs > DEFAULT_TIMEOUT_MS);
        // Forwarded client requests: the client times out on its own, just stop tracking.
        forwardedClientRequests.values().removeIf(f -> now - f.creationTimeMs() > f.timeoutMs());
        return failPending(pendingServerServerRequests, h -> now - h.creationTimeMs > h.request.getTimeoutMs(), "timed out");
    }

    /**
     * Disconnect sweep: fails the master/slave requests sent over a lost connection with a
     * {@link RequestOutcomeUnknownException}. On a slave, client requests forwarded to the master are
     * answered to their clients with {@link GenericResponsePacket#STATUS_OUTCOME_UNKNOWN} as well.
     * Call on the server thread.
     *
     * @param slaveID      on the master: the slave that disconnected; on a slave: {@code null} (master lost)
     * @param disconnectMs when the connection was lost; requests created later are kept
     * @return number of failed requests (including forwarded client requests)
     */
    public int failRequestsOnConnectionLost(@Nullable String slaveID, long disconnectMs) {
        int failed = 0;
        if(slaveID == null) {
            for (var it = forwardedClientRequests.entrySet().iterator(); it.hasNext(); ) {
                var entry = it.next();
                ForwardedRequest f = entry.getValue();
                if (f.creationTimeMs() > disconnectMs)
                    continue;
                it.remove();
                sendFailureToClient(f.playerUUID(), entry.getKey(), f.requestTypeID(), GenericResponsePacket.STATUS_OUTCOME_UNKNOWN);
                failed++;
            }
        }
        return failed + failPending(pendingServerServerRequests,
                h -> h.creationTimeMs <= disconnectMs && Objects.equals(slaveID, h.targetSlaveID), "connection lost");
    }

    private static int failPending(Map<UUID, ServerRequestHolder<?,?>> pending, Predicate<ServerRequestHolder<?,?>> fail, String reason) {
        int failed = 0;
        for (var it = pending.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            ServerRequestHolder<?,?> h = entry.getValue();
            if (!fail.test(h))
                continue;
            it.remove();
            h.responseFuture.completeExceptionally(new RequestOutcomeUnknownException(
                    "ARRS request " + reason + ", outcome unknown: " + h.request.getRequestTypeID() + " " + entry.getKey()));
            failed++;
        }
        return failed;
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

        switch (responsePacket.getStatus()) {
            case GenericResponsePacket.STATUS_NOT_SENT -> requestData.responseFuture.completeExceptionally(new RequestNotSentException(
                    "ARRS request not forwarded to the master: " + requestData.request.getRequestTypeID()));
            case GenericResponsePacket.STATUS_OUTCOME_UNKNOWN -> requestData.responseFuture.completeExceptionally(new RequestOutcomeUnknownException(
                    "ARRS request connection lost, outcome unknown: " + requestData.request.getRequestTypeID() + " " + requestId));
            default -> requestData.processResponse(responsePacket.getData());
        }
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
            forwardedClientRequests.remove(requestId);
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
                GenericResponsePacket relay = responsePacket;
                if (responsePacket.getStatus() == GenericResponsePacket.STATUS_OK) {
                    RegistryFriendlyByteBuf data = responsePacket.getData();
                    int readerIndex = data.readerIndex();
                    RegistryFriendlyByteBuf transformed = request.transformResponseOnSlave(data, context.senderPlayerUUID);
                    if (transformed == data)
                        data.readerIndex(readerIndex); // the hook may have decoded it; relay all of it
                    else
                        relay = new GenericResponsePacket(requestId, responsePacket.getRequestTypeID(), transformed);
                }
                request.getManager().getNetworkManager().sendToClient(targetPlayer, relay);
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
