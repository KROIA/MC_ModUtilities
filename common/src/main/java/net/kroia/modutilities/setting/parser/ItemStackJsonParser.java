package net.kroia.modutilities.setting.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kroia.modutilities.ItemUtilities;
import net.kroia.modutilities.persistence.ServerSaveable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;

public class ItemStackJsonParser implements CustomJsonParser<ItemStack>{
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

        }
        private ItemData(ItemStack stack, EnchantmentData[] enchantments, PotionData potion)
        {
            this.stack = stack;
            this.enchantments = enchantments;
            this.potion = potion;
        }
        public ItemStack getItemStack()
        {
           return null;
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

    private final NBTJsonParser nbtJsonParser = new NBTJsonParser();
    @Override
    public JsonElement toJson(ItemStack stack) {
       return null;
    }

    @Override
    public ItemStack fromJson(JsonElement json) {
        return null;

        /*ItemData itemData = new ItemData();
        if(itemData.fromJson(json)) {
            return itemData.getItemStack();
        } else {
            throw new IllegalArgumentException("Invalid JSON element for ItemStack");
        }*/
    }
}
