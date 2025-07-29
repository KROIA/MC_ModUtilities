package net.kroia.modutilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

public class JsonUtilities {
    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson GSON = new GsonBuilder().create();


    public static String toPrettyString(JsonElement jsonElement) {
        return GSON_PRETTY.toJson(jsonElement);
    }
    public static String toString(JsonElement jsonElement) {
        return GSON.toJson(jsonElement);
    }
}
