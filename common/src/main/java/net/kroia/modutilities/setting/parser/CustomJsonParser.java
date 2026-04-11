package net.kroia.modutilities.setting.parser;

import com.google.gson.JsonElement;

public interface CustomJsonParser<T> {
    JsonElement toJson(T value);
    T fromJson(JsonElement json);
}
