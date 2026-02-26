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

public class ItemStackJsonParser implements CustomJsonParser<ItemStack>{
    /*
    private static class EnchantmentData implements ServerSaveable
    {
        public String enchantmentID;
        public int level;

        public EnchantmentData()
        {}
        public EnchantmentData(String enchantmentID, int level)
        {
            this.enchantmentID = enchantmentID;
            this.level = level;
        }

        @Override
        public boolean save(CompoundTag tag) {
            tag.putString("id", enchantmentID);
            tag.putInt("lvl", level);
            return true;
        }

        @Override
        public boolean load(CompoundTag tag) {
            enchantmentID = tag.getString("id");
            level = tag.getInt("lvl");
            return true;
        }

        public JsonElement toJson()
        {
            JsonObject data = new JsonObject();
            data.addProperty("id", enchantmentID);
            data.addProperty("lvl", level);
            return data;
        }

        public boolean fromJson(JsonElement json) {
            if (json.isJsonObject()) {
                JsonObject data = json.getAsJsonObject();
                if (data.has("id") && data.has("lvl")) {
                    enchantmentID = data.get("id").getAsString();
                    level = data.get("lvl").getAsInt();
                    return true;
                }
            }
            return false;
        }
    }
    private static class PotionData implements ServerSaveable
    {
        public String potionID;
        //public int amplifier;

        public PotionData()
        {}
        public PotionData(String potionID)
        {
            this.potionID = potionID;
        }

        @Override
        public boolean save(CompoundTag tag) {
            tag.putString("Potion", potionID);
            return true;
        }

        @Override
        public boolean load(CompoundTag tag) {
            potionID = tag.getString("Potion");
            return true;
        }

        public void toJson(JsonObject data)
        {
            data.addProperty("Potion", potionID);
        }
        public boolean fromJson(JsonElement json)
        {
            if (!json.isJsonObject()) {
                return false;
            }
            JsonObject data = json.getAsJsonObject();
            if (data.has("Potion")) {
                potionID = data.get("Potion").getAsString();
                //amplifier = data.get("amplifier").getAsInt();
                return true;
            }
            return false;
        }
    }

    public static class ItemData// implements ServerSaveable
    {
        public ItemStack stack;
        public EnchantmentData[] enchantments;
        public PotionData potion;

        public ItemData()
        {}
        public ItemData(ItemStack stack)
        {
            // Not implemented
            this.stack = stack;
            throw new RuntimeException("ItemData is not implemented in the modutilities");
        }
        private ItemData(ItemStack stack, EnchantmentData[] enchantments, PotionData potion)
        {
            this.stack = stack;
            this.enchantments = enchantments;
            this.potion = potion;
        }
        public ItemStack getItemStack()
        {
           return stack;
        }

        boolean fromJson(JsonElement json) {
            if (json.isJsonObject()) {
                JsonObject data = json.getAsJsonObject();
                if (data.has("itemID")) {
                    stack = ItemUtilities.createItemStackFromId(data.get("itemID").getAsString());
                }
                if (data.has("StoredEnchantments")) {
                    JsonArray enchantmentsTag = data.getAsJsonArray("StoredEnchantments");
                    enchantments = new EnchantmentData[enchantmentsTag.size()];
                    for (int i = 0; i < enchantmentsTag.size(); i++) {
                        EnchantmentData enchantment = new EnchantmentData();
                        enchantment.fromJson(enchantmentsTag.get(i));
                        enchantments[i] = enchantment;
                    }
                } else {
                    enchantments = new EnchantmentData[0];
                }
                if (data.has("Potion")) {
                    potion = new PotionData();
                    potion.fromJson(data);
                } else {
                    potion = null;
                }
                return true;
            }
            return false;
        }

    }
*/
    /**
     * Serializes an ItemStack to a JsonElement.
     * Output format:
     * {
     *   "id": "minecraft:diamond_sword",
     *   "count": 1,
     *   "components": { ... }  // only if non-default components exist
     * }
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
     * Deserializes an ItemStack from a JsonElement produced by toJson().
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

    private JsonObject nbtToJson(CompoundTag tag) {
        JsonObject json = new JsonObject();
        for (String key : tag.getAllKeys()) {
            Tag value = tag.get(key);
            json.add(key, nbtTagToJson(value));
        }
        return json;
    }

    private JsonElement nbtTagToJson(Tag tag) {
        switch (tag.getId()) {
            case Tag.TAG_BYTE:
            case Tag.TAG_SHORT:
            case Tag.TAG_INT:
            case Tag.TAG_LONG:
            case Tag.TAG_FLOAT:
            case Tag.TAG_DOUBLE: {
                // All numeric types
                String str = tag.getAsString();
                try { return new JsonPrimitive(Double.parseDouble(str)); }
                catch (NumberFormatException e) { return new JsonPrimitive(str); }
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

    private CompoundTag jsonToNbt(JsonObject json) {
        CompoundTag tag = new CompoundTag();
        for (var entry : json.entrySet()) {
            tag.put(entry.getKey(), jsonElementToNbt(entry.getValue()));
        }
        return tag;
    }

    private Tag jsonElementToNbt(JsonElement element) {
        if (element.isJsonNull()) {
            return net.minecraft.nbt.StringTag.valueOf("");
        } else if (element.isJsonPrimitive()) {
            JsonPrimitive prim = element.getAsJsonPrimitive();
            if (prim.isBoolean()) {
                return net.minecraft.nbt.ByteTag.valueOf(prim.getAsBoolean() ? (byte) 1 : (byte) 0);
            } else if (prim.isNumber()) {
                Number n = prim.getAsNumber();
                // Prefer int, fall back to double
                double d = n.doubleValue();
                if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) < Integer.MAX_VALUE) {
                    return net.minecraft.nbt.IntTag.valueOf(n.intValue());
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
            return jsonToNbt(element.getAsJsonObject());
        }
        return net.minecraft.nbt.StringTag.valueOf(element.getAsString());
    }
}
