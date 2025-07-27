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
    private final T defaultValue;
    private final Type type;
    private final String name;
    private final DataEvent<T> event = new DataEvent<>();
    private final CustomJsonParser<T> customJsonParser;

    public Setting(String name, T initialValue, Type type) {
        this.name = name;
        this.defaultValue = initialValue;
        this.value = initialValue;
        this.type = type;
        this.customJsonParser = null; // No custom parser by default
    }
    public Setting(String name, T initialValue, Type type, CustomJsonParser<T> customJsonParser) {
        this.name = name;
        this.defaultValue = initialValue;
        this.value = initialValue;
        this.type = type;
        this.customJsonParser = customJsonParser;
    }

    public T get() {
        return value;
    }

    public void set(T newValue) {
        if (!Objects.equals(this.value, newValue)) {
            this.value = newValue;
            notifyListeners();
        }
    }

    public String getName() {
        return name;
    }
    public Type getType() {
        return type;
    }

    public void addListener(Consumer<T> listener) {
        event.addListener(listener);
    }

    public void removeListener(Consumer<T> listener) {
        event.removeListener(listener);
    }

    public void setToDefaultValue() {
        set(defaultValue);
    }


    private void notifyListeners() {
        event.notifyListeners(value);
    }

    public T getDefaultValue() {
        return defaultValue;
    }
    public CustomJsonParser<T> getCustomJsonParser() {
        return customJsonParser;
    }
    public T getCustomParsedToData(JsonElement json)
    {
        assert customJsonParser != null;
        return customJsonParser.fromJson(json);
    }
    public JsonElement getCustomParsedToJson() {
        assert customJsonParser != null;
        return customJsonParser.toJson(value);
    }

    @Override
    public String toString() {
        return "Setting<" + type + "> " + name + " = " + value;
    }
}
