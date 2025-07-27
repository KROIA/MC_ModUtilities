package net.kroia.modutilities.setting;


import net.minecraft.world.item.enchantment.ThornsEnchantment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ModSettings {

    public Consumer<String> errorLogger = System.err::println;
    public BiConsumer<String, Throwable> errorLoggerThrowable = (error, throwable) -> {
        if (errorLogger != null) {
            errorLogger.accept(error + "\nError: " + throwable.getMessage() + "\n" + Arrays.toString(throwable.getStackTrace()));
        }
    };
    public Consumer<String> infoLogger = System.out::println;
    private final List<SettingsGroup> allGroups = new ArrayList<>();
    //private String settingsFileName;
    private final String name;

    public ModSettings(String name) {
        this.name = name;
    }

    public void setLogger(Consumer<String> errorLogger, Consumer<String> infoLogger) {
        this.errorLogger = errorLogger;
        this.infoLogger = infoLogger;
    }
    public void setLogger(Consumer<String> errorLogger, BiConsumer<String, Throwable>errorLoggerThrowable, Consumer<String> infoLogger) {
        this.errorLogger = errorLogger;
        this.errorLoggerThrowable = errorLoggerThrowable;
        this.infoLogger = infoLogger;
    }

    protected <T extends SettingsGroup> T createGroup(T group) {
        assert allGroups != null;
        allGroups.add(group);
        return group;
    }

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
        if(infoLogger != null)
            infoLogger.accept(name+" saved to JSON file: " + filePath);
        return true;
    }
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
        if(infoLogger != null)
            infoLogger.accept(name+" loaded from JSON file: " + filePath);
        return true;
    }
}
