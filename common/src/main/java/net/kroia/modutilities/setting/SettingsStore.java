package net.kroia.modutilities.setting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kroia.modutilities.setting.parser.CustomJsonParser;

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
        JsonElement json = toJson(groups);

        try (Writer writer = new FileWriter(file)) {
            gson.toJson(json, writer);
        }
    }

    // Loads setting values from a file into existing groups
    public void loadFromFile(List<SettingsGroup> groups, String filePath) throws IOException {
        File file = new File(filePath);
        loadFromFile(groups, file);
    }
    public void loadFromFile(List<SettingsGroup> groups, File file) throws IOException {
        if (!file.exists()) return;

        JsonObject root;
        try (Reader reader = new FileReader(file)) {
            root = gson.fromJson(reader, JsonObject.class);
        }
        catch(FileNotFoundException e) {
            throw new IOException("Settings file not found: " + file.getAbsolutePath(), e);
        }
        catch (Exception e) {
            throw new IOException("Failed to read settings from file: " + file.getAbsolutePath(), e);
        }
        if (root == null) {
            throw new IOException("Failed to parse settings file: " + file.getAbsolutePath());
        }
        fromJson(groups, root);


        /*for (SettingsGroup group : groups) {
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
        }*/
    }

    public String toJsonString(List<SettingsGroup> groups) {
        JsonElement json = toJson(groups);
        return gson.toJson(json);
    }

    public String toJsonString(SettingsGroup group) {
        JsonElement json = toJson(group);
        return gson.toJson(json);
    }

    public JsonElement toJson(SettingsGroup group)
    {
        JsonObject groupJson = new JsonObject();
        for (Setting<?> setting : group.getAllSettings()) {
            JsonElement valueJson = null;
            CustomJsonParser<?> customParser = setting.getCustomJsonParser();
            if (customParser != null) {
                valueJson = setting.getCustomParsedToJson();
            } else {
                valueJson = gson.toJsonTree(setting.get());
            }
            groupJson.add(setting.getName(), valueJson);
        }
        return groupJson;
    }
    public JsonElement toJson(List<SettingsGroup> groups) {
        JsonObject root = new JsonObject();

        for (SettingsGroup group : groups) {
            JsonElement groupJson = toJson(group);
            root.add(group.getName(), groupJson);
        }
        return root;
    }

    public SettingsGroup fromJson(SettingsGroup loader, JsonElement json)
    {
        if(json == null || !json.isJsonObject()) {
            throw new IllegalArgumentException("Invalid JSON element for SettingsGroup");
        }
        JsonObject groupJson = json.getAsJsonObject();

        for (Setting<?> setting : loader.getAllSettings()) {
            JsonElement valueJson = groupJson.get(setting.getName());
            if (valueJson != null && !valueJson.isJsonNull()) {
                CustomJsonParser<?> customParser = setting.getCustomJsonParser();
                Object value;
                if (customParser != null) {
                    value = setting.getCustomParsedToData(valueJson);
                } else {
                    value = gson.fromJson(valueJson, setting.getType());
                }

                // Type-safe cast using getType()
                setValueWithCast(setting, value);
            }
        }
        return loader;
    }

    public List<SettingsGroup> fromJson(List<SettingsGroup> groups, JsonElement json) {
        if(json == null || !json.isJsonObject()) {
            throw new IllegalArgumentException("Invalid JSON element for SettingsGroup list");
        }
        JsonObject root = json.getAsJsonObject();
        for( SettingsGroup group : groups) {
            JsonElement groupJson = root.get(group.getName());
            if (groupJson != null && groupJson.isJsonObject()) {
                fromJson(group, groupJson);
            }
            // Missing groups are silently skipped — defaults are preserved.
            // This allows new groups added in mod updates to load gracefully.
        }
        return groups;
    }







    @SuppressWarnings("unchecked")
    private <T> void setValueWithCast(Setting<?> setting, Object value) {
        // Convert numeric types when Gson hands back a Double for a non-Double target type.
        // Gson defaults numbers to Double when type info is loose (e.g. Object), causing
        // ClassCastException when later read as Integer/Long/Float.
        if (value instanceof Number num && setting.getType() instanceof Class<?> cls) {
            if (cls == Integer.class || cls == int.class) value = num.intValue();
            else if (cls == Long.class || cls == long.class) value = num.longValue();
            else if (cls == Float.class || cls == float.class) value = num.floatValue();
            else if (cls == Short.class || cls == short.class) value = num.shortValue();
            else if (cls == Byte.class || cls == byte.class) value = num.byteValue();
            else if (cls == Double.class || cls == double.class) value = num.doubleValue();
        }
        Setting<T> typedSetting = (Setting<T>) setting;
        typedSetting.set((T) value);
    }


}
