package net.kroia.modutilities.networking.client_server.arrs;

import net.kroia.modutilities.networking.client_server.ClientServerPacketManager;
import net.kroia.modutilities.networking.server_server.ServerServerManager;
import net.kroia.modutilities.networking.server_server.ServerServerPacketRegistry;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


/**
 * Asynchronous Request Response System (ARRS) for handling requests and responses in a networked environment.
 * This system allows for sending requests to the server or client and receiving responses.
 * It is designed to be used with the NetworkManager from this utilities mod for packet handling.
 *
 * This class combines the components required for the ARRS, including the RequestRegistry and RequestManager.
 *
 */
public class AsynchronousRequestResponseSystem {

    /**
     * The RequestRegistry instance that holds all registered requests.
     */
    private static final RequestRegistry REGISTRY = new RequestRegistry();

    /**
     * The RequestManager instance that manages the requests and responses.
     * This is initialized in the setup method and used to send requests and handle responses.
     */
    private static RequestManager REQUEST_MANAGER;


    /**
     * Sets up the ARRS with the provided NetworkManager.
     * This method initializes the RequestManager and registers the necessary packets for request and response handling.
     *
     * @param networkManager The NetworkManager to use for packet handling.
     */
    public static void setup(@NotNull ClientServerPacketManager networkManager) {
        if (REQUEST_MANAGER != null) {
            return;
        }
        REQUEST_MANAGER = new RequestManager(networkManager);

        Map<String, RequestRegistry.RegistryData<?,?>> requests = REGISTRY.getRegistry();
        for(Map.Entry<String, RequestRegistry.RegistryData<?,?>> entry : requests.entrySet())
        {
            GenericRequest<?, ?> request = entry.getValue().request;
            if(request != null)
                request.setManager(REQUEST_MANAGER);
        }

        // Register packets for ARRS
        networkManager.registerC2S(GenericRequestPacket.TYPE, GenericRequestPacket.STREAM_CODEC, GenericRequestPacket.HANDLER);
        ServerServerPacketRegistry.register(GenericRequestPacket.TYPE, GenericRequestPacket.STREAM_CODEC, GenericRequestPacket.HANDLER);
        networkManager.registerS2C(GenericResponsePacket.TYPE,  GenericResponsePacket.STREAM_CODEC, GenericResponsePacket.HANDLER);
        ServerServerPacketRegistry.register(GenericResponsePacket.TYPE,  GenericResponsePacket.STREAM_CODEC, GenericResponsePacket.HANDLER);
    }


    /**
     * Registers a GenericRequest in the registry.
     *
     * @param request The GenericRequest static instance to register.
     * @param <IN>    The input type of the request.
     * @param <OUT>   The output type of the request.
     * @return The registered request, or null if there is already a request with the same RequestTypeID registered.
     */
    public static <IN, OUT> GenericRequest<IN, OUT> register(@NotNull GenericRequest<IN, OUT> request)
    {
        request.setManager(REQUEST_MANAGER);
        return REGISTRY.register(request);
    }

    /**
     * Unregisters a GenericRequest from the registry.
     *
     * @param request The GenericRequest to unregister.
     * @param <IN>    The input type of the request.
     * @param <OUT>   The output type of the request.
     */
    public static <IN, OUT> void unregister(@NotNull GenericRequest<IN, OUT> request)
    {
        request.setManager(null);
        REGISTRY.unregister(request);
    }

    /**
     * Unregisters a GenericRequest from the registry by its RequestTypeID.
     *
     * @param requestTypeID The ID of the request type to unregister.
     */
    public static void unregister(@NotNull String requestTypeID)
    {
        var request = REGISTRY.getRegisteredRequest(requestTypeID);
        if(request != null) {
            request.setManager(null);
            REGISTRY.unregister(requestTypeID);
        }
    }

    /**
     * Clears the registry, removing all registered requests.
     */
    public static void clearRegistry()
    {
        var requests = REGISTRY.getRegistry();
        for(Map.Entry<String, RequestRegistry.RegistryData<?,?>> entry : requests.entrySet())
        {
            GenericRequest<?, ?> request = entry.getValue().request;
            if(request != null)
                request.setManager(null);
        }
        REGISTRY.clear();
    }

    /**
     * Retrieves a registered GenericRequest by its RequestTypeID.
     *
     * @param requestTypeID The ID of the request type to retrieve.
     * @return The registered GenericRequest, or null if no request with the given ID is found.
     */
    public static GenericRequest<?, ?> getRegisteredRequest(@NotNull String requestTypeID)
    {
        return REGISTRY.getRegisteredRequest(requestTypeID);
    }



    /**
     * Sends a request to the server and registers a response handler.
     * The request will be sent using the NetworkManager set in this RequestManager.
     *
     * @param request The request to send.
     * @param input The input data for the request delivered to the receiver.
     * @param responseHandler The handler to call when the response is received.
     * @param <IN> The type of input data.
     * @param <OUT> The type of output data provided by the provider
     */
    public static <IN, OUT> void sendRequestToServer(@NotNull GenericRequest<IN, OUT> request, IN input, @NotNull Consumer<OUT> responseHandler) {
        checkManagerExists();
        REQUEST_MANAGER.sendRequestToServer(request, input, responseHandler);
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
    public static <IN, OUT> void sendRequestToClient(@NotNull GenericRequest<IN, OUT> request, IN input, @NotNull ServerPlayer target, @NotNull BiConsumer<OUT, ServerPlayer> responseHandler) {
        checkManagerExists();
        REQUEST_MANAGER.sendRequestToClient(request, input, target, responseHandler);
    }

    /**
     * Gets the number of requests that have been sent to the server but have not yet received a response.
     * @return The number of pending server requests.
     */
    public static int getPendingServerRequestsCount() {
        checkManagerExists();
        return REQUEST_MANAGER.getPendingServerRequestsCount();
    }

    /**
     * Gets the number of requests that have been sent to the client but have not yet received a response.
     * @return The number of pending client requests.
     */
    public static int getPendingClientRequestsCount() {
        checkManagerExists();
        return REQUEST_MANAGER.getPendingClientRequestsCount();
    }

    /**
     * Clears all pending server requests.
     */
    public static void clearPendingServerRequests() {
        checkManagerExists();
        REQUEST_MANAGER.clearPendingServerRequests();
    }

    /**
     * Clears all pending client requests.
     */
    public static void clearPendingClientRequests() {
        checkManagerExists();
        REQUEST_MANAGER.clearPendingClientRequests();
    }

    /**
     * Clears all pending requests, both server and client.
     */
    public static void clearPendingRequests() {
        checkManagerExists();
        REQUEST_MANAGER.clearPendingRequests();
    }



    /**
     * Processes a response packet received on the client side.
     * This method will look up the request data using the request ID and call the response handler.
     * This function is called automatically when a GenericResponsePacket is received on the client side.
     *
     * @param responsePacket The response packet received from the server.
     */
    public static void processResponseOnClient(@NotNull GenericResponsePacket responsePacket)
    {
        REQUEST_MANAGER.processResponseOnClient(responsePacket);
    }

    /**
     * Processes a response packet received on the server side.
     * This method will look up the request data using the request ID and call the response handler.
     * This function is called automatically when a GenericResponsePacket is received on the server side.
     *
     * @param responsePacket The response packet received from the client.
     * @param player The player who sent the request.
     */
    public static void processResponseOnServer(@NotNull GenericResponsePacket responsePacket,@NotNull ServerPlayer player)
    {
        REQUEST_MANAGER.processResponseOnServer(responsePacket, player);
    }

    private static void checkManagerExists()
    {
        if(REQUEST_MANAGER == null)
            throw new IllegalStateException("""
                    RequestManager is not set. Cannot send request to server.
                    Make sure to setup the ARRS correctly.
                    Call AsynchronousRequestResponseSystem.setup() once on the client and server side.
                    Also register the Request classes with AsynchronousRequestResponseSystem.register(new CustomRequest()) once on the client and server side.
                    """);
    }
}
