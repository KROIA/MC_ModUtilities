package net.kroia.modutilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.nio.file.Files;

/**
 * Thin wrapper around two shared {@link Gson} instances configured with consistent
 * settings, providing convenience methods for serializing and parsing
 * {@link JsonElement}s.
 * <p>
 * Two formatters are exposed: a pretty-printing one ({@link #toPrettyString(JsonElement)})
 * for human-readable output and a compact one ({@link #toString(JsonElement)}) for
 * storage and transmission.
 */
public class JsonUtilities {
    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson GSON = new GsonBuilder().create();



    /**
     * Serializes the given JSON element using pretty-printed formatting (newlines and indentation).
     *
     * @param jsonElement the element to serialize
     * @return the JSON text in human-readable form
     */
    public static String toPrettyString(JsonElement jsonElement) {
        return GSON_PRETTY.toJson(jsonElement);
    }
    /**
     * Serializes the given JSON element in compact form, with no extra whitespace.
     *
     * @param jsonElement the element to serialize
     * @return the JSON text in compact form
     */
    public static String toString(JsonElement jsonElement) {
        return GSON.toJson(jsonElement);
    }

    /**
     * Parses the given JSON text into a {@link JsonElement} tree.
     *
     * @param jsonString the JSON text to parse
     * @return the parsed element tree
     * @throws com.google.gson.JsonSyntaxException if the input is not well-formed JSON
     */
    public static JsonElement fromString(String jsonString) {
        return GSON.fromJson(jsonString, JsonElement.class);
    }
}
