package net.kroia.modutilities.setting.parser;

import com.google.gson.*;
import net.kroia.modutilities.ItemUtilities;
import net.kroia.modutilities.persistence.ServerSaveable;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * {@link CustomJsonParser} implementation for Minecraft {@link ItemStack} values.
 * Encodes the item ID, count, and any non-default {@link DataComponentPatch} components
 * (custom NBT, enchantments, custom name, lore, etc.) into a JSON object.
 */
public class ItemStackJsonParser implements CustomJsonParser<ItemStack>{

    /**
     * Serializes an {@link ItemStack} to a {@link JsonElement}.
     * <p>
     * Output format:
     * <pre>
     * {
     *   "id": "minecraft:diamond_sword",
     *   "count": 1,
     *   "components": { ... }  // only if non-default components exist
     * }
     * </pre>
     *
     * @param stack The item stack to serialize. May be null or empty (returns {@link JsonNull}).
     * @return A {@link JsonObject} describing the stack, or {@link JsonNull#INSTANCE} for empty stacks.
     */
    public JsonElement toJson(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return JsonNull.INSTANCE;
        }

        JsonObject json = new JsonObject();

        // Item ID
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        json.addProperty("id", itemId.toString());

        // Count
        json.addProperty("count", stack.getCount());

        // Data components (custom NBT, enchantments, name, lore, etc.)
        DataComponentPatch patch = stack.getComponentsPatch();
        if (!patch.isEmpty()) {
            // Encode the component patch using NbtOps
            Tag componentTag = DataComponentPatch.CODEC
                    .encodeStart(NbtOps.INSTANCE, patch)
                    .getOrThrow();

            if (componentTag instanceof CompoundTag compoundTag) {
                // Convert CompoundTag to JsonObject
                JsonObject componentsJson = nbtToJson(compoundTag);
                json.add("components", componentsJson);
            }
        }

        return json;
    }

    /**
     * Deserializes an {@link ItemStack} from a {@link JsonElement} produced by {@link #toJson(ItemStack)}.
     *
     * @param json The JSON element to parse. May be null or {@link JsonNull} (returns {@link ItemStack#EMPTY}).
     * @return The reconstructed {@link ItemStack}, or {@link ItemStack#EMPTY} for null/null-JSON input.
     * @throws JsonParseException if the JSON is malformed, missing the required {@code id} field,
     *                            references an unknown item, or carries an invalid component patch.
     */
    public ItemStack fromJson(JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return ItemStack.EMPTY;
        }

        if (!json.isJsonObject()) {
            throw new JsonParseException("Expected a JsonObject for ItemStack, got: " + json);
        }

        JsonObject obj = json.getAsJsonObject();

        // --- Item ID ---
        if (!obj.has("id")) {
            throw new JsonParseException("ItemStack JSON missing 'id' field");
        }
        ResourceLocation itemId = ResourceLocation.tryParse(obj.get("id").getAsString());
        if (itemId == null) {
            throw new JsonParseException("Invalid item resource location: " + obj.get("id").getAsString());
        }

        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == Items.AIR && !itemId.toString().equals("minecraft:air")) {
            throw new JsonParseException("Unknown item: " + itemId);
        }

        // --- Count ---
        int count = obj.has("count") ? obj.get("count").getAsInt() : 1;

        // --- Build base ItemStack ---
        ItemStack stack = new ItemStack(item, count);

        // --- Components ---
        if (obj.has("components")) {
            JsonObject componentsJson = obj.getAsJsonObject("components");
            CompoundTag componentTag = jsonToNbt(componentsJson);

            DataComponentPatch patch = DataComponentPatch.CODEC
                    .parse(NbtOps.INSTANCE, componentTag)
                    .getOrThrow();

            stack.applyComponents(patch);
        }

        return stack;
    }

    // -------------------------------------------------------------------------
    // Helpers: Convert between CompoundTag and JsonObject
    // -------------------------------------------------------------------------

    /**
     * Key used inside typed-number JSON wrappers to distinguish them from
     * real NBT compound tags.  The wrapper format is:
     * <pre>{"nbt_type": "byte", "value": 5}</pre>
     * During deserialization, a JSON object containing exactly these two keys
     * is treated as a typed number; any other JSON object is treated as a
     * compound tag.  Plain JSON numbers (old format) fall back to the legacy
     * int-or-double heuristic for backwards compatibility.
     */
    private static final String NBT_TYPE_KEY = "nbt_type";
    private static final String NBT_VALUE_KEY = "value";

    /**
     * Converts a {@link CompoundTag} to a {@link JsonObject}, preserving
     * the exact numeric type of every NBT tag via typed-number wrappers.
     *
     * @param tag The compound tag to convert.
     * @return A JSON object representing the tag.
     */
    public JsonObject nbtToJson(CompoundTag tag) {
        JsonObject json = new JsonObject();
        for (String key : tag.getAllKeys()) {
            Tag value = tag.get(key);
            json.add(key, nbtTagToJson(value));
        }
        return json;
    }

    /**
     * Converts a single NBT {@link Tag} to a {@link JsonElement}.
     * Numeric tags are encoded as wrapper objects {@code {"nbt_type": "...", "value": ...}}
     * to preserve their exact type through serialization.
     *
     * @param tag The NBT tag to convert.
     * @return The corresponding JSON element.
     */
    public JsonElement nbtTagToJson(Tag tag) {
        switch (tag.getId()) {
            case Tag.TAG_BYTE: {
                JsonObject wrapper = new JsonObject();
                wrapper.addProperty(NBT_TYPE_KEY, "byte");
                wrapper.addProperty(NBT_VALUE_KEY, ((net.minecraft.nbt.NumericTag) tag).getAsByte());
                return wrapper;
            }
            case Tag.TAG_SHORT: {
                JsonObject wrapper = new JsonObject();
                wrapper.addProperty(NBT_TYPE_KEY, "short");
                wrapper.addProperty(NBT_VALUE_KEY, ((net.minecraft.nbt.NumericTag) tag).getAsShort());
                return wrapper;
            }
            case Tag.TAG_INT: {
                JsonObject wrapper = new JsonObject();
                wrapper.addProperty(NBT_TYPE_KEY, "int");
                wrapper.addProperty(NBT_VALUE_KEY, ((net.minecraft.nbt.NumericTag) tag).getAsInt());
                return wrapper;
            }
            case Tag.TAG_LONG: {
                JsonObject wrapper = new JsonObject();
                wrapper.addProperty(NBT_TYPE_KEY, "long");
                wrapper.addProperty(NBT_VALUE_KEY, ((net.minecraft.nbt.NumericTag) tag).getAsLong());
                return wrapper;
            }
            case Tag.TAG_FLOAT: {
                JsonObject wrapper = new JsonObject();
                wrapper.addProperty(NBT_TYPE_KEY, "float");
                wrapper.addProperty(NBT_VALUE_KEY, ((net.minecraft.nbt.NumericTag) tag).getAsFloat());
                return wrapper;
            }
            case Tag.TAG_DOUBLE: {
                JsonObject wrapper = new JsonObject();
                wrapper.addProperty(NBT_TYPE_KEY, "double");
                wrapper.addProperty(NBT_VALUE_KEY, ((net.minecraft.nbt.NumericTag) tag).getAsDouble());
                return wrapper;
            }
            case Tag.TAG_STRING:
                return new JsonPrimitive(tag.getAsString());
            case Tag.TAG_COMPOUND:
                return nbtToJson((CompoundTag) tag);
            case Tag.TAG_LIST: {
                net.minecraft.nbt.ListTag list = (net.minecraft.nbt.ListTag) tag;
                JsonArray arr = new JsonArray();
                for (Tag element : list) {
                    arr.add(nbtTagToJson(element));
                }
                return arr;
            }
            case Tag.TAG_BYTE_ARRAY: {
                net.minecraft.nbt.ByteArrayTag bat = (net.minecraft.nbt.ByteArrayTag) tag;
                JsonArray arr = new JsonArray();
                for (byte b : bat.getAsByteArray()) arr.add(b);
                return arr;
            }
            case Tag.TAG_INT_ARRAY: {
                net.minecraft.nbt.IntArrayTag iat = (net.minecraft.nbt.IntArrayTag) tag;
                JsonArray arr = new JsonArray();
                for (int i : iat.getAsIntArray()) arr.add(i);
                return arr;
            }
            case Tag.TAG_LONG_ARRAY: {
                net.minecraft.nbt.LongArrayTag lat = (net.minecraft.nbt.LongArrayTag) tag;
                JsonArray arr = new JsonArray();
                for (long l : lat.getAsLongArray()) arr.add(l);
                return arr;
            }
            default:
                return new JsonPrimitive(tag.getAsString());
        }
    }

    /**
     * Converts a {@link JsonObject} back to a {@link CompoundTag}.
     * Recognizes both the new typed-number wrapper format and legacy plain
     * JSON numbers for backwards compatibility.
     *
     * @param json The JSON object to convert.
     * @return The corresponding compound tag.
     */
    public CompoundTag jsonToNbt(JsonObject json) {
        CompoundTag tag = new CompoundTag();
        for (var entry : json.entrySet()) {
            tag.put(entry.getKey(), jsonElementToNbt(entry.getValue()));
        }
        return tag;
    }

    /**
     * Checks whether a JSON object is a typed-number wrapper produced by the
     * new serialization format: exactly two keys, {@code "nbt_type"} and
     * {@code "value"}.
     */
    private static boolean isTypedNumberWrapper(JsonObject obj) {
        return obj.size() == 2
                && obj.has(NBT_TYPE_KEY)
                && obj.has(NBT_VALUE_KEY)
                && obj.get(NBT_TYPE_KEY).isJsonPrimitive()
                && obj.getAsJsonPrimitive(NBT_TYPE_KEY).isString()
                && obj.get(NBT_VALUE_KEY).isJsonPrimitive()
                && obj.getAsJsonPrimitive(NBT_VALUE_KEY).isNumber();
    }

    /**
     * Reconstructs an NBT numeric tag from a typed-number wrapper object.
     *
     * @param obj A JSON object of the form {@code {"nbt_type": "<type>", "value": <number>}}
     * @return The corresponding NBT tag with the correct numeric type
     */
    private Tag typedNumberToNbt(JsonObject obj) {
        String type = obj.get(NBT_TYPE_KEY).getAsString();
        Number value = obj.get(NBT_VALUE_KEY).getAsNumber();
        return switch (type) {
            case "byte" -> net.minecraft.nbt.ByteTag.valueOf(value.byteValue());
            case "short" -> net.minecraft.nbt.ShortTag.valueOf(value.shortValue());
            case "int" -> net.minecraft.nbt.IntTag.valueOf(value.intValue());
            case "long" -> net.minecraft.nbt.LongTag.valueOf(value.longValue());
            case "float" -> net.minecraft.nbt.FloatTag.valueOf(value.floatValue());
            case "double" -> net.minecraft.nbt.DoubleTag.valueOf(value.doubleValue());
            default -> net.minecraft.nbt.DoubleTag.valueOf(value.doubleValue());
        };
    }

    /**
     * Converts a {@link JsonElement} back to an NBT {@link Tag}.
     * <ul>
     *   <li>Typed-number wrappers ({@code {"nbt_type": "...", "value": ...}}) reconstruct
     *       the exact NBT numeric type (new format).</li>
     *   <li>Plain JSON numbers use a legacy heuristic: whole numbers within int range become
     *       {@code IntTag}, everything else becomes {@code DoubleTag} (backwards compatibility).</li>
     *   <li>JSON objects without the wrapper keys become {@code CompoundTag}.</li>
     * </ul>
     *
     * @param element The JSON element to convert.
     * @return The corresponding NBT tag.
     */
    public Tag jsonElementToNbt(JsonElement element) {
        if (element.isJsonNull()) {
            return net.minecraft.nbt.StringTag.valueOf("");
        } else if (element.isJsonPrimitive()) {
            JsonPrimitive prim = element.getAsJsonPrimitive();
            if (prim.isBoolean()) {
                return net.minecraft.nbt.ByteTag.valueOf(prim.getAsBoolean() ? (byte) 1 : (byte) 0);
            } else if (prim.isNumber()) {
                // Legacy format: plain JSON number without type info.
                // Use the old heuristic for backwards compatibility.
                Number n = prim.getAsNumber();
                double d = n.doubleValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    if (d >= Integer.MIN_VALUE && d <= Integer.MAX_VALUE) {
                        return net.minecraft.nbt.IntTag.valueOf(n.intValue());
                    }
                    if (d >= Long.MIN_VALUE && d <= Long.MAX_VALUE) {
                        return net.minecraft.nbt.LongTag.valueOf(n.longValue());
                    }
                }
                return net.minecraft.nbt.DoubleTag.valueOf(d);
            } else {
                return net.minecraft.nbt.StringTag.valueOf(prim.getAsString());
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            for (JsonElement e : arr) {
                list.add(jsonElementToNbt(e));
            }
            return list;
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            // New format: typed-number wrapper {"nbt_type": "...", "value": ...}
            if (isTypedNumberWrapper(obj)) {
                return typedNumberToNbt(obj);
            }
            // Regular compound tag
            return jsonToNbt(obj);
        }
        return net.minecraft.nbt.StringTag.valueOf(element.getAsString());
    }
}
