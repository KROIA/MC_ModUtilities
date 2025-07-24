package net.kroia.modutilities.setting;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


/**
 * Represents a group of settings that can be registered and managed together.
 * Each setting can have a default value and a type.
 * <p>
 * Usage example:
 * <pre>
 * class UtilitiesSettings extends SettingsGroup {
 *      public final Setting<Integer> exampleSetting = settings.registerSetting("exampleSetting", 42, Integer.class);
 *
 *  }
 * </pre>
 */
public class SettingsGroup {

    /**
     * A logger for error messages. Can be overridden to use a custom logger.
     * Default is System.err, which prints errors to the standard error stream.
     */
    public static Consumer<String> errorLogger = System.err::println; // Default error logger, can be overridden

    /**
     * A list of settings registered in this group.
     * Each setting is an instance of the Setting class.
     */
    private final List<Setting<?>> settings = new ArrayList<>();

    /**
     * The name of this settings group.
     * Used for identification and serialization purposes.
     */
    private final String name;

    public SettingsGroup(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Registers a new setting with the specified name, default value, and type.
     * The setting is added to the list of settings in this group.
     *
     * @param settingName  The name of the setting.
     * @param defaultValue The default value of the setting.
     * @param type         The class type of the setting's value.
     * @param <T>          The type of the setting's value.
     * @return The registered Setting instance.
     */
    protected <T> Setting<T> registerSetting(String settingName, T defaultValue, Type type) {
        Setting<T> setting = new Setting<>(settingName, defaultValue, type);
        settings.add(setting);
        return setting;
    }

    /**
     * Retrieves a setting by its name.
     * If the setting does not exist, it returns null.
     *
     * @param settingName The name of the setting to retrieve.
     * @return The Setting instance if found, otherwise null.
     */
    public List<Setting<?>> getAllSettings() {
        return settings;
    }

    /**
     * @usage
     * SettingsGroup settings = new SettingsGroup("ExampleSettings");
     * settings.forEachSetting(s -> System.out.println(" - " + s.get()));
     */
    public void forEachSetting(java.util.function.Consumer<Setting<?>> action) {
        settings.forEach(action);
    }

    /**
     * Sets all settings in this group to their default values.
     * This method iterates through each setting and calls its setToDefaultValue method.
     */
    public void setToDefaultValue() {
        for (Setting<?> setting : settings) {
            setting.setToDefaultValue();
        }
    }

    @Override
    public String toString() {
        SettingsStore store = new SettingsStore();
        return store.toJsonString(this);
    }
}
