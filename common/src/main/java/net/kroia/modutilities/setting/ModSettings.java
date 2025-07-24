package net.kroia.modutilities.setting;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class ModSettings {

    public static Consumer<String> errorLogger = System.err::println;
    public static Consumer<String> infoLogger = System.out::println;
    private final List<SettingsGroup> allGroups = new ArrayList<>();
    private String settingsFileName;


    private String name;

    public static void setLogger(Consumer<String> errorLogger, Consumer<String> infoLogger) {
        ModSettings.errorLogger = errorLogger;
        ModSettings.infoLogger = infoLogger;

        SettingsGroup.errorLogger = errorLogger;
        Setting.errorLogger = errorLogger;
    }

    public ModSettings(String name) {
        this(name, "settings.json");
    }
    public ModSettings(String name, String settingsFileName) {
        this.name = name;
        if(settingsFileName != null && !settingsFileName.isEmpty()) {
            this.settingsFileName = settingsFileName;
        }
    }
    protected <T extends SettingsGroup> T createGroup(T group) {
        assert allGroups != null;
        allGroups.add(group);
        return group;
    }

    public String getSettingsFileName() {
        return settingsFileName;
    }
    abstract public String getSettingsFilePath();

    public boolean saveSettings()
    {
        SettingsStore store = new SettingsStore();
        String path = getSettingsFilePath() + "/" + getSettingsFileName();
        try {
            store.saveToFile(allGroups, path);
        }
        catch (Exception e) {
            if(errorLogger != null)
                errorLogger.accept("Failed to save "+name+" to path: " + path + "\nError:\n" + e.getMessage());
            return false;
        }
        if(infoLogger != null)
            infoLogger.accept(name+" saved to JSON file: " + path);
        return true;
    }
    public boolean loadSettings()
    {
        SettingsStore store = new SettingsStore();
        String path = getSettingsFilePath() + "/" + getSettingsFileName();
        try {
            store.loadFromFile(allGroups,path);
        } catch (Exception e) {
            if(errorLogger != null)
                errorLogger.accept("Failed to load "+name+" from path: " +path + "\nError:\n" + e.getMessage());
            return false;
        }
        if(infoLogger != null)
            infoLogger.accept(name+" loaded from JSON file: " + path);
        return true;
    }
}
