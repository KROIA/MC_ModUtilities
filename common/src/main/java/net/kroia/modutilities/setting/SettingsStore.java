package net.kroia.modutilities.setting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.*;
import java.util.List;

public class SettingsStore {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void saveToFile(List<SettingsGroup> groups, String filePath) throws IOException {
        File file = new File(filePath);
        saveToFile(groups, file);
    }

    // Saves multiple setting groups into a JSON file
    public void saveToFile(List<SettingsGroup> groups, File file) throws IOException {
        JsonObject root = new JsonObject();

        for (SettingsGroup group : groups) {
            JsonObject groupJson = new JsonObject();
            for (Setting<?> setting : group.getAllSettings()) {
                JsonElement valueJson = gson.toJsonTree(setting.get());
                groupJson.add(setting.getName(), valueJson);
            }
            root.add(group.getName(), groupJson);
        }

        try (Writer writer = new FileWriter(file)) {
            gson.toJson(root, writer);
        }
    }

    public String toJsonString(List<SettingsGroup> groups) {
        JsonObject root = new JsonObject();

        for (SettingsGroup group : groups) {
            JsonObject groupJson = new JsonObject();
            for (Setting<?> setting : group.getAllSettings()) {
                JsonElement valueJson = gson.toJsonTree(setting.get());
                groupJson.add(setting.getName(), valueJson);
            }
            root.add(group.getName(), groupJson);
        }

        return gson.toJson(root);
    }

    public String toJsonString(SettingsGroup group) {
        JsonObject groupJson = new JsonObject();
        for (Setting<?> setting : group.getAllSettings()) {
            JsonElement valueJson = gson.toJsonTree(setting.get());
            groupJson.add(setting.getName(), valueJson);
        }
        return gson.toJson(groupJson);
    }


    public void loadFromFile(List<SettingsGroup> groups, String filePath) throws IOException {
        File file = new File(filePath);
        loadFromFile(groups, file);
    }


    // Loads setting values from a file into existing groups
    public void loadFromFile(List<SettingsGroup> groups, File file) throws IOException {
        if (!file.exists()) return;

        JsonObject root;
        try (Reader reader = new FileReader(file)) {
            root = gson.fromJson(reader, JsonObject.class);
        }

        for (SettingsGroup group : groups) {
            JsonObject groupJson = root.getAsJsonObject(group.getName());
            if (groupJson == null) continue;

            for (Setting<?> setting : group.getAllSettings()) {
                JsonElement valueJson = groupJson.get(setting.getName());
                if (valueJson != null && !valueJson.isJsonNull()) {
                    Object value = gson.fromJson(valueJson, setting.getType());

                    // Type-safe cast using getType()
                    setValueWithCast(setting, value);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void setValueWithCast(Setting<?> setting, Object value) {
        Setting<T> typedSetting = (Setting<T>) setting;
        typedSetting.set((T) value);
    }


}
