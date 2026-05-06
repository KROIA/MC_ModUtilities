package net.kroia.modutilities.setting;

import com.google.gson.JsonElement;
import net.kroia.modutilities.event.DataEvent;
import net.kroia.modutilities.setting.parser.CustomJsonParser;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * A generic setting class that holds a value of type T, its default value, and provides methods to get, set, and listen for changes.
 *
 * @param <T> the type of the setting value
 */
public class Setting<T> {
    private T value;
    private int valueHashCode;
    private final T defaultValue;
    private final Type type;
    private final String name;
    private final DataEvent<T> event = new DataEvent<>();
    private final CustomJsonParser<T> customJsonParser;

    /**
     * Constructs a new Setting with the given name, initial value, and type.
     * The initial value is also used as the default value.
     *
     * @param name         The unique name of this setting within its group.
     * @param initialValue The initial (and default) value of the setting. May be null.
     * @param type         The reflective type of the setting's value, used by Gson for (de)serialization.
     */
    public Setting(String name, T initialValue, Type type) {
        this.name = name;
        this.defaultValue = initialValue;
        this.value = initialValue;
        this.valueHashCode = initialValue != null ? initialValue.hashCode() : 0;
        this.type = type;
        this.customJsonParser = null; // No custom parser by default
    }

    /**
     * Constructs a new Setting with a custom JSON parser for non-Gson-friendly types.
     *
     * @param name             The unique name of this setting within its group.
     * @param initialValue     The initial (and default) value of the setting. May be null.
     * @param type             The reflective type of the setting's value.
     * @param customJsonParser A custom parser used to (de)serialize the value to/from JSON.
     */
    public Setting(String name, T initialValue, Type type, CustomJsonParser<T> customJsonParser) {
        this.name = name;
        this.defaultValue = initialValue;
        this.value = initialValue;
        this.valueHashCode = initialValue != null ? initialValue.hashCode() : 0;
        this.type = type;
        this.customJsonParser = customJsonParser;
    }

    /**
     * Returns the current value of the setting.
     *
     * @return The current value, or {@code null} if it has been explicitly set to null.
     */
    public T get() {
        return value;
    }

    /**
     * Sets a new value for this setting.
     * If the new value differs from the current one (compared via {@code hashCode} and
     * {@link Objects#equals(Object, Object)}), all registered listeners are notified.
     *
     * @param newValue The new value to assign. May be null.
     *
     * @apiNote
     * Change detection relies on {@code hashCode} and {@link Objects#equals(Object, Object)};
     * it will NOT detect in-place mutation of mutable objects (e.g. modifying fields of a
     * shared object reference). To trigger listener notification after mutating a mutable
     * value, pass a new instance or a copy to {@code set()}.
     */
    public void set(T newValue) {
        // Check if the new value is different from the current value
        int newValueHashCode = 0;
        if(newValue != null) {
            newValueHashCode = newValue.hashCode();
        }
        if (newValueHashCode != valueHashCode || !Objects.equals(newValue, value)) {
            this.value = newValue;
            this.valueHashCode = newValueHashCode;
            notifyListeners();
        }
    }

    /**
     * Returns the unique name of this setting within its group.
     *
     * @return The setting's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the reflective type of this setting's value.
     *
     * @return The {@link Type} used for (de)serialization.
     */
    public Type getType() {
        return type;
    }

    /**
     * Registers a listener that will be invoked whenever {@link #set(Object)} changes the value.
     *
     * @param listener The {@link Consumer} that receives the new value when changed.
     */
    public void addListener(Consumer<T> listener) {
        event.addListener(listener);
    }

    /**
     * Unregisters a previously added change listener.
     *
     * @param listener The listener instance to remove.
     */
    public void removeListener(Consumer<T> listener) {
        event.removeListener(listener);
    }

    /**
     * Resets the setting to its default value (the initial value passed to the constructor).
     * Listeners are notified if this differs from the current value.
     */
    public void setToDefaultValue() {
        set(defaultValue);
    }


    private void notifyListeners() {
        event.notifyListeners(value);
    }

    /**
     * Returns the default value of this setting (the initial value passed to the constructor).
     *
     * @return The default value. May be null.
     */
    public T getDefaultValue() {
        return defaultValue;
    }

    /**
     * Returns the custom JSON parser associated with this setting, if any.
     *
     * @return The {@link CustomJsonParser}, or {@code null} if standard Gson serialization is used.
     */
    public CustomJsonParser<T> getCustomJsonParser() {
        return customJsonParser;
    }

    /**
     * Deserializes a JSON element into this setting's value type using its custom parser.
     *
     * @param json The JSON element to parse.
     * @return The deserialized value of type T.
     * @throws AssertionError if no custom parser was configured for this setting.
     */
    public T getCustomParsedToData(JsonElement json)
    {
        assert customJsonParser != null;
        return customJsonParser.fromJson(json);
    }

    /**
     * Serializes the current value to a JSON element using this setting's custom parser.
     *
     * @return A {@link JsonElement} representing the current value.
     * @throws AssertionError if no custom parser was configured for this setting.
     */
    public JsonElement getCustomParsedToJson() {
        assert customJsonParser != null;
        return customJsonParser.toJson(value);
    }

    @Override
    public String toString() {
        return "Setting<" + type + "> " + name + " = " + value;
    }
}
