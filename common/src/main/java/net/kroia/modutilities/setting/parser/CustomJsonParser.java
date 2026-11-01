package net.kroia.modutilities.setting.parser;

import com.google.gson.JsonElement;

/**
 * Pluggable JSON serializer for setting value types that Gson cannot handle natively
 * (e.g. {@code ItemStack}, {@code Tag}). Implementations are attached to a
 * {@link net.kroia.modutilities.setting.Setting} via its constructor.
 *
 * @param <T> The value type handled by this parser.
 */
public interface CustomJsonParser<T> {
    /**
     * Serializes a value of type T into a {@link JsonElement}.
     *
     * @param value The value to serialize. Implementations may decide how to handle null.
     * @return The JSON representation of the value.
     */
    JsonElement toJson(T value);

    /**
     * Deserializes a {@link JsonElement} back into a value of type T.
     *
     * @param json The JSON element to parse.
     * @return The reconstructed value.
     */
    T fromJson(JsonElement json);
}
