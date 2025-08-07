package net.kroia.modutilities.networking.arrs;


import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * RequestRegistry is a class that manages the registration and unregistration of Request objects.
 * It allows for storing requests by their type ID and provides methods to retrieve, register, and unregister requests.
 *
 * @param <IN>  The input type of the request.
 * @param <OUT> The output type of the request.
 */
public class RequestRegistry
{
    public static class RegistryData<IN, OUT>
    {
        GenericRequest<IN, OUT> request;
    }

    private final Map<String, RegistryData<?,?>> registry = new java.util.HashMap<>();

    /**
     * Registers a GenericRequest in the registry.
     *
     * @param request The GenericRequest static instance to register.
     * @param <IN>    The input type of the request.
     * @param <OUT>   The output type of the request.
     * @return The registered request, or null if there is already a request with the same RequestTypeID registered.
     */
    public <IN, OUT> GenericRequest<IN, OUT> register(@NotNull GenericRequest<IN, OUT> request)
    {
        RegistryData<IN, OUT> data = new RegistryData<>();
        data.request = request;

        if (registry.containsKey(request.getRequestTypeID()))
        {
            return null; // already registered
        }
        registry.put(request.getRequestTypeID(), data);
        return request;
    }


    /**
     * Unregisters a GenericRequest from the registry by its RequestTypeID.
     * @param requestTypeID
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
}