package net.kroia.modutilities.setting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kroia.modutilities.setting.parser.CustomJsonParser;

import java.io.*;
import java.util.List;

/**
 * Handles JSON serialization and deserialization of one or more {@link SettingsGroup}s.
 * Uses a pretty-printing {@link Gson} instance internally and consults each setting's
 * {@link CustomJsonParser} (if set) for non-Gson-friendly types.
 */
public class SettingsStore {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();



    /**
     * Saves the given settings groups to a JSON file at the specified path.
     *
     * @param groups   The list of groups whose values should be serialized.
     * @param filePath The destination file path.
     * @throws IOException if writing to the file fails.
     */
    public void saveToFile(List<SettingsGroup> groups, String filePath) throws IOException {
        File file = new File(filePath);
        saveToFile(groups, file);
    }

    /**
     * Saves the given settings groups to a JSON file.
     *
     * @param groups The list of groups whose values should be serialized.
     * @param file   The destination file.
     * @throws IOException if writing to the file fails.
     */
    public void saveToFile(List<SettingsGroup> groups, File file) throws IOException {
        JsonElement json = toJson(groups);

        try (Writer writer = new FileWriter(file)) {
            gson.toJson(json, writer);
        }
    }

    /**
     * Loads setting values from a JSON file into the provided groups.
     * If the file does not exist, the call is a no-op and the existing values are preserved.
     *
     * @param groups   The groups to populate with values from the file.
     * @param filePath The source file path.
     * @throws IOException if the file cannot be read or parsed.
     */
    public void loadFromFile(List<SettingsGroup> groups, String filePath) throws IOException {
        File file = new File(filePath);
        loadFromFile(groups, file);
    }

    /**
     * Loads setting values from a JSON file into the provided groups.
     * If the file does not exist, the call is a no-op and the existing values are preserved.
     *
     * @param groups The groups to populate with values from the file.
     * @param file   The source file.
     * @throws IOException if the file cannot be read or parsed.
     */
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

    /**
     * Serializes the given list of groups to a pretty-printed JSON string.
     *
     * @param groups The groups to serialize.
     * @return The JSON string representation.
     */
    public String toJsonString(List<SettingsGroup> groups) {
        JsonElement json = toJson(groups);
        return gson.toJson(json);
    }

    /**
     * Serializes a single group to a pretty-printed JSON string.
     *
     * @param group The group to serialize.
     * @return The JSON string representation of the group's settings.
     */
    public String toJsonString(SettingsGroup group) {
        JsonElement json = toJson(group);
        return gson.toJson(json);
    }

    /**
     * Serializes a single group's settings to a {@link JsonElement}.
     * Each setting is encoded using its custom parser if present, otherwise via Gson.
     *
     * @param group The group to serialize.
     * @return A {@link JsonObject} keyed by setting name.
     */
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
    /**
     * Serializes a list of groups to a {@link JsonElement}, keyed by group name.
     *
     * @param groups The groups to serialize.
     * @return A {@link JsonObject} where each key is a group name and each value is the group's serialized form.
     */
    public JsonElement toJson(List<SettingsGroup> groups) {
        JsonObject root = new JsonObject();

        for (SettingsGroup group : groups) {
            JsonElement groupJson = toJson(group);
            root.add(group.getName(), groupJson);
        }
        return root;
    }

    /**
     * Loads values from a JSON object into the given group.
     * Settings absent from the JSON keep their current value; numeric types are coerced
     * to the target class to avoid Gson defaulting to {@code Double}.
     *
     * @param loader The group to populate.
     * @param json   A {@link JsonObject} mapping setting names to their JSON-encoded values.
     * @return The same {@code loader} instance, for chaining.
     * @throws IllegalArgumentException if {@code json} is null or not a JSON object.
     */
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

    /**
     * Loads values from a JSON object into the given list of groups.
     * Groups missing from the JSON are silently skipped, preserving their current/default values.
     * This enables graceful upgrades when new groups are added in mod updates.
     *
     * @param groups The groups to populate.
     * @param json   A {@link JsonObject} keyed by group name.
     * @return The same {@code groups} list, for chaining.
     * @throws IllegalArgumentException if {@code json} is null or not a JSON object.
     *
     * @apiNote
     * Missing groups are intentionally not treated as errors — this allows older settings
     * files to load successfully even after the mod adds new {@link SettingsGroup}s.
     */
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
