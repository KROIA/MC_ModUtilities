package net.kroia.modutilities.setting;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Top-level container for a mod's settings. Aggregates multiple {@link SettingsGroup}s and
 * delegates JSON persistence to {@link SettingsStore}, with pluggable error and debug loggers.
 */
public class ModSettings {

    public Consumer<String> errorLogger = System.err::println;
    public BiConsumer<String, Throwable> errorLoggerThrowable = (error, throwable) -> {
        if (errorLogger != null) {
            errorLogger.accept(error + "\nError: " + throwable.getMessage() + "\n" + Arrays.toString(throwable.getStackTrace()));
        }
    };
    public Consumer<String> debugLogger = System.out::println;
    private final List<SettingsGroup> allGroups = new ArrayList<>();
    //private String settingsFileName;
    private final String name;

    /**
     * Constructs a new ModSettings container.
     *
     * @param name A descriptive name used in log messages (typically the mod name).
     */
    public ModSettings(String name) {
        this.name = name;
    }

    /**
     * Sets the error and debug log handlers used during save/load operations.
     *
     * @param errorLogger Receives error messages (no exception).
     * @param debugLogger Receives debug/info messages.
     */
    public void setLogger(Consumer<String> errorLogger, Consumer<String> debugLogger) {
        this.errorLogger = errorLogger;
        this.debugLogger = debugLogger;
    }

    /**
     * Sets the error, error-with-throwable, and debug log handlers used during save/load operations.
     *
     * @param errorLogger          Receives plain error messages.
     * @param errorLoggerThrowable Receives error messages along with the causing {@link Throwable}.
     * @param debugLogger          Receives debug/info messages.
     */
    public void setLogger(Consumer<String> errorLogger, BiConsumer<String, Throwable>errorLoggerThrowable, Consumer<String> debugLogger) {
        this.errorLogger = errorLogger;
        this.errorLoggerThrowable = errorLoggerThrowable;
        this.debugLogger = debugLogger;
    }

    /**
     * Registers a {@link SettingsGroup} with this container so it is included in save/load operations.
     *
     * @param group The group instance to register.
     * @param <T>   The concrete group subtype.
     * @return The same {@code group} instance, for fluent assignment to fields.
     */
    protected <T extends SettingsGroup> T createGroup(T group) {
        assert allGroups != null;
        allGroups.add(group);
        return group;
    }

    /**
     * Saves all registered groups to a JSON file.
     * Errors are reported to the configured error logger and result in a {@code false} return value.
     *
     * @param filePath The destination file path.
     * @return {@code true} on success, {@code false} if an error occurred.
     */
    public boolean saveSettings(String filePath)
    {
        SettingsStore store = new SettingsStore();
        try {
            store.saveToFile(allGroups, filePath);
        }
        catch (Exception e) {
            if(errorLoggerThrowable != null)
                errorLoggerThrowable.accept("Failed to save "+name+" to path: " + filePath + "\nError:\n" + e.getMessage(), e);
            return false;
        }
        if(debugLogger != null)
            debugLogger.accept(name+" saved to JSON file: " + filePath);
        return true;
    }
    /**
     * Loads values for all registered groups from a JSON file.
     * Errors are reported to the configured error logger and result in a {@code false} return value.
     * If the file does not exist, the call still returns {@code true} and groups keep their defaults.
     *
     * @param filePath The source file path.
     * @return {@code true} on success (or if the file did not exist), {@code false} on error.
     */
    public boolean loadSettings(String filePath)
    {
        SettingsStore store = new SettingsStore();
        try {
            store.loadFromFile(allGroups, filePath);
        } catch (Exception e) {
            if(errorLoggerThrowable != null)
                errorLoggerThrowable.accept("Failed to load "+name+" from path: " +filePath + "\nError:\n" + e.getMessage(), e);
            return false;
        }
        if(debugLogger != null)
            debugLogger.accept(name+" loaded from JSON file: " + filePath);
        return true;
    }
}
