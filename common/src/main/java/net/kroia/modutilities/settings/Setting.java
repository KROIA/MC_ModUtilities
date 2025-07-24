package net.kroia.modutilities.settings;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * A generic setting class that holds a value of type T, its default value, and provides methods to get, set, and listen for changes.
 *
 * @param <T> the type of the setting value
 */
public class Setting<T> {
    public static Consumer<String> errorLogger = System.err::println;
    private T value;
    private final T defaultValue;
    private final Type type;
    private final String name;
    private final List<Consumer<T>> listeners = new ArrayList<>();

    public Setting(String name, T initialValue, Type type) {
        this.name = name;
        this.defaultValue = initialValue;
        this.value = initialValue;
        this.type = type;
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
        listeners.add(listener);
    }

    public void removeListener(Consumer<T> listener) {
        listeners.remove(listener);
    }

    public void setToDefaultValue() {
        set(defaultValue);
    }


    private void notifyListeners() {
        for (Consumer<T> listener : listeners) {
            listener.accept(value);
        }
    }

    @Override
    public String toString() {
        return "Setting<" + type + "> " + name + " = " + value;
    }
}
