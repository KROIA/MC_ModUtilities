package net.kroia.modutilities.networking.client_server.arrs;


import net.kroia.modutilities.ModUtilitiesMod;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Manages registration and lookup of {@link GenericRequest} instances keyed by their
 * {@link GenericRequest#getRequestTypeID() request type ID}. Backed by a thread-safe
 * {@link java.util.concurrent.ConcurrentHashMap}.
 */
public class RequestRegistry
{
    /**
     * Holder for a registered request entry.
     *
     * @param <IN>  The input type of the request.
     * @param <OUT> The output type of the request.
     */
    public static class RegistryData<IN, OUT>
    {
        GenericRequest<IN, OUT> request;
    }

    private final Map<String, RegistryData<?,?>> registry = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Registers a Request in the registry.
     *
     * @param request The static Request instance to register.
     * @param <IN>    The input type of the request.
     * @param <OUT>   The output type of the request.
     * @return The registered request, or null if there is already a request with the same RequestTypeID registered.
     */
    public <IN, OUT> GenericRequest<IN, OUT> register(@NotNull GenericRequest<IN, OUT> request)
    {
        RegistryData<IN, OUT> data = new RegistryData<>();
        data.request = request;

        String requestTypeID = request.getRequestTypeID();
        if (registry.containsKey(requestTypeID))
        {
            error("Request with ID "+requestTypeID+" is already registered!");
            return null; // already registered
        }
        registry.put(requestTypeID, data);
        return request;
    }


    /**
     * Unregisters a GenericRequest from the registry by its RequestTypeID.
     *
     * @param requestTypeID The ID of the request type to unregister.
     */
    public void unregister(@NotNull String requestTypeID)
    {
        registry.remove(requestTypeID);
    }

    /**
     * Unregisters a GenericRequest from the registry.
     *
     * @param request The GenericRequest to unregister.
     * @param <IN>    The input type of the request.
     * @param <OUT>   The output type of the request.
     */
    public <IN, OUT> void unregister(@NotNull GenericRequest<IN, OUT> request)
    {
        unregister(request.getRequestTypeID());
    }

    /**
     * Clears the registry, removing all registered requests.
     */
    public void clear()
    {
        registry.clear();
    }

    /**
     * Retrieves the entire registry map.
     *
     * @return A map containing all registered requests, keyed by their RequestTypeID.
     */
    public Map<String, RegistryData<?,?>> getRegistry()
    {
        return registry;
    }

    /**
     * Retrieves a registered GenericRequest by its RequestTypeID.
     *
     * @param requestTypeID The ID of the request type to retrieve.
     * @return The registered GenericRequest, or null if no request with the given ID is found.
     */
    public GenericRequest<?, ?> getRegisteredRequest(@NotNull String requestTypeID)
    {
        RegistryData<?, ?> data = registry.get(requestTypeID);
        if (data == null)
        {
            return null; // not found
        }
        return data.request;
    }

    private void error(String msg, Throwable e) {
        ModUtilitiesMod.LOGGER.error("[RequestRegistry] " + msg, e);
    }
    private void error(String msg) {
        ModUtilitiesMod.LOGGER.error("[RequestRegistry] " + msg);
    }
    private void warn(String msg) {
        ModUtilitiesMod.LOGGER.warn("[RequestRegistry] " + msg);
    }
}