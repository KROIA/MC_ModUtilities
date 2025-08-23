package net.kroia.modutilities.networking.streaming;

import net.kroia.modutilities.ModUtilitiesMod;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * StreamRegistry is a class that manages the registration and unregistration of Stream objects.
 * It allows for storing streams by their type ID and provides methods to retrieve, register, and unregister streams.
 *
 * @param <CONTEXT_DATA> The type of context data associated with a stream.
 *                       This is typically used to provide additional information about
 *                       the stream's context and gets sent on start of a stream request.
 * @param <DATA>         The type of data that the stream will handle.
 */
public class StreamRegistry {

    public static class RegistryData<CONTEXT_DATA, DATA>
    {
        GenericStream<CONTEXT_DATA, DATA> stream;
    }


    private final Map<String, RegistryData<?,?>> registry = new java.util.HashMap<>();


    /**
     * Registers a Stream in the registry.
     * @param stream The static Stream instance to register.
     * @return The registered stream, or null if there is already a stream with the same StreamTypeID registered.
     * @param <CONTEXT_DATA> The type of context data associated with the stream.4
     * @param <DATA>         The type of data that the stream will handle.
     */
    public <CONTEXT_DATA, DATA> GenericStream<CONTEXT_DATA, DATA> register(@NotNull GenericStream<CONTEXT_DATA, DATA> stream)
    {
        RegistryData<CONTEXT_DATA, DATA> data = new RegistryData<>();
        data.stream = stream;

        String streamTypeID = stream.getStreamTypeID();
        if (registry.containsKey(streamTypeID))
        {
            error("Stream with ID "+streamTypeID+" is already registered!");
            return null; // already registered
        }
        registry.put(streamTypeID, data);
        return stream;
    }

    /**
     * Unregisters a Stream from the registry by its type ID.
     * @param streamTypeID The type ID of the stream to unregister.
     */
    public void unregister(@NotNull String streamTypeID)
    {
        registry.remove(streamTypeID);
    }

    /**
     * Unregisters a Stream from the registry.
     * @param stream The Stream instance to unregister.
     * @param <CONTEXT_DATA> The type of context data associated with the stream.
     * @param <DATA>         The type of data that the stream will handle.
     */
    public <CONTEXT_DATA, DATA> void unregister(@NotNull GenericStream<CONTEXT_DATA, DATA> stream)
    {
        registry.remove(stream.getStreamTypeID());
    }

    /**
     * Clears the registry, removing all registered streams.
     */
    public void clear()
    {
        registry.clear();
    }

    /**
     * Retrieves the registry map containing all registered streams.
     * @return A map where the keys are stream type IDs and the values are RegistryData objects containing the streams.
     */
    public Map<String, RegistryData<?,?>> getRegistry()
    {
        return registry;
    }


    /**
     * Retrieves a registered Stream by its type ID.
     * @param streamTypeID The ID of the stream type to retrieve.
     * @return The registered Stream, or null if no stream with the given ID is found.
     */
    public GenericStream<?, ?> getRegisteredStream(@NotNull String streamTypeID)
    {
        RegistryData<?, ?> data = registry.get(streamTypeID);
        if (data == null)
        {
            return null; // not found
        }
        return data.stream;
    }


    private void error(String msg, Throwable e) {
        ModUtilitiesMod.LOGGER.error("[StreamRegistry] " + msg, e);
    }
    private void error(String msg) {
        ModUtilitiesMod.LOGGER.error("[StreamRegistry] " + msg);
    }
    private void warn(String msg) {
        ModUtilitiesMod.LOGGER.warn("[StreamRegistry] " + msg);
    }
}
