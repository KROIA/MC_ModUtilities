package net.kroia.modutilities.setting.parser;

import com.google.gson.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public class NBTJsonParser implements CustomJsonParser<Tag>{

    /**
     * Convert a CompoundTag (NBT) to a JsonElement (Gson).
     */
    @Override
    public JsonElement toJson(Tag tag) {
        if (tag instanceof CompoundTag compound) {
            JsonObject obj = new JsonObject();
            Set<String> keys = compound.getAllKeys();
            for (String key : keys) {
                obj.add(key, toJson(compound.get(key)));
            }
            return obj;
        } else if (tag instanceof ListTag list) {
            JsonArray arr = new JsonArray();
            for (Tag element : list) {
                arr.add(toJson(element));
            }
            return arr;
        } else if (tag instanceof StringTag str) {
            return new JsonPrimitive(str.getAsString());
        } else if (tag instanceof NumericTag num) {
            // NumericTag covers ByteTag, ShortTag, IntTag, LongTag, FloatTag, DoubleTag
            return new JsonPrimitive(num.getAsNumber());
        } else if (tag instanceof ByteArrayTag arr) {
            JsonArray jsonArray = new JsonArray();
            for (byte b : arr.getAsByteArray()) {
                jsonArray.add(b);
            }
            return jsonArray;
        } else if (tag instanceof IntArrayTag arr) {
            JsonArray jsonArray = new JsonArray();
            for (int i : arr.getAsIntArray()) {
                jsonArray.add(i);
            }
            return jsonArray;
        } else if (tag instanceof LongArrayTag arr) {
            JsonArray jsonArray = new JsonArray();
            for (long l : arr.getAsLongArray()) {
                jsonArray.add(l);
            }
            return jsonArray;
        } else if (tag == null || tag instanceof EndTag) {
            return JsonNull.INSTANCE;
        } else {
            throw new IllegalArgumentException("Unsupported NBT type: " + tag.getClass());
        }
    }

    /**
     * Convert a JsonElement (Gson) to a CompoundTag (NBT).
     * Expects the root to be a JsonObject.
     */
    @Override
    public Tag fromJson(JsonElement json) {
        if (json.isJsonObject()) {
            CompoundTag compound = new CompoundTag();
            for (var entry : json.getAsJsonObject().entrySet()) {
                compound.put(entry.getKey(), fromJson(entry.getValue()));
            }
            return compound;
        } else if (json.isJsonArray()) {
            JsonArray arr = json.getAsJsonArray();
            ListTag list = new ListTag();
            for (JsonElement e : arr) {
                list.add(fromJson(e));
            }
            return list;
        } else if (json.isJsonPrimitive()) {
            JsonPrimitive prim = json.getAsJsonPrimitive();
            if (prim.isBoolean()) {
                return ByteTag.valueOf((byte) (prim.getAsBoolean() ? 1 : 0));
            } else if (prim.isNumber()) {
                Number n = prim.getAsNumber();
                // Default to Long if no decimals, Double otherwise
                if (Math.floor(n.doubleValue()) == n.doubleValue()) {
                    long lv = n.longValue();
                    if (lv >= Byte.MIN_VALUE && lv <= Byte.MAX_VALUE) return ByteTag.valueOf((byte) lv);
                    if (lv >= Short.MIN_VALUE && lv <= Short.MAX_VALUE) return ShortTag.valueOf((short) lv);
                    if (lv >= Integer.MIN_VALUE && lv <= Integer.MAX_VALUE) return IntTag.valueOf((int) lv);
                    return LongTag.valueOf(lv);
                } else {
                    double dv = n.doubleValue();
                    float fv = (float) dv;
                    if ((double) fv == dv) return FloatTag.valueOf(fv);
                    return DoubleTag.valueOf(dv);
                }
            } else if (prim.isString()) {
                return StringTag.valueOf(prim.getAsString());
            }
        } else if (json.isJsonNull()) {
            return EndTag.INSTANCE;
        }
        throw new IllegalArgumentException("Unsupported JSON element: " + json);
    }
}
