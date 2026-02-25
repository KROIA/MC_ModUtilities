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
            this.stack = stack;
            CompoundTag tag = stack.getTag();
            if(tag == null)
                return;
            ArrayList<EnchantmentData> ench = new ArrayList<>();
            int i = 0;
            if (tag.contains("StoredEnchantments", Tag.TAG_LIST)) {
                ListTag enchantments = tag.getList("StoredEnchantments", Tag.TAG_COMPOUND);
                for (Tag enchantmentTag : enchantments) {
                    CompoundTag enchantment = (CompoundTag) enchantmentTag;
                    EnchantmentData enchantmentData = new EnchantmentData();
                    enchantmentData.load(enchantment);
                    ench.add(enchantmentData);
                    i++;
                }
                this.enchantments = new EnchantmentData[i];
                for(int j = 0; j < ench.size(); j++)
                {
                    this.enchantments[j] = ench.get(j);
                }
            }

            if (tag.contains("Potion", Tag.TAG_STRING)) {
                PotionData potion = new PotionData();
                potion.load(tag);
                this.potion = potion;
            }
            else {
                this.potion = null;
            }
        }
        private ItemData(ItemStack stack, EnchantmentData[] enchantments, PotionData potion)
        {
            this.stack = stack;
            this.enchantments = enchantments;
            this.potion = potion;
        }
        public ItemStack getItemStack()
        {
            if(enchantments == null && potion == null)
                return stack;
            CompoundTag tag = new CompoundTag();
            if(enchantments != null) {
                if (enchantments.length > 0) {
                    ListTag enchantmentsTag = new ListTag();
                    for (EnchantmentData enchantment : enchantments) {
                        CompoundTag enchantmentTag = new CompoundTag();
                        enchantment.save(enchantmentTag);
                        enchantmentsTag.add(enchantmentTag);
                    }
                    tag.put("StoredEnchantments", enchantmentsTag);
                }
            }

            if(potion != null) {
                potion.save(tag);
            }

            if(!tag.isEmpty())
                stack.setTag(tag);
            return stack;
        }

        JsonElement toJson()
        {
            JsonObject data = new JsonObject();
            data.addProperty("itemID", ItemUtilities.getItemIDStr(stack.getItem()));
            if(enchantments != null) {
                if (enchantments.length > 0) {
                    JsonArray enchantmentsTag = new JsonArray();
                    for (EnchantmentData enchantment : enchantments) {
                        enchantmentsTag.add(enchantment.toJson());
                    }
                    data.add("StoredEnchantments", enchantmentsTag);
                }
            }
            if(potion != null)
                potion.toJson(data);
            return data;
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
        //return new ItemData(stack).toJson();

        JsonObject stackData = new JsonObject();
        stackData.addProperty("itemID", ItemUtilities.getItemIDStr(stack.getItem()));
        CompoundTag tag = stack.getTag();
        if(tag != null)
            stackData.add("nbt", nbtJsonParser.toJson(tag));
        return stackData;
    }

    @Override
    public ItemStack fromJson(JsonElement json) {
        if (!json.isJsonObject()) {
            throw new IllegalArgumentException("Invalid JSON element for ItemStack");
        }
        JsonObject data = json.getAsJsonObject();

        if (!data.has("itemID")) {
            throw new IllegalArgumentException("JSON element does not contain 'itemID' for ItemStack");
        }
        String itemID = data.get("itemID").getAsString();

        ItemStack stack = ItemUtilities.createItemStackFromId(itemID);
        if (stack == null || stack.isEmpty() && !stack.is((Items.AIR))) {
            throw new IllegalArgumentException("Invalid itemID: " + itemID);
        }
        if (data.has("nbt")) {
            JsonElement nbtElement = data.get("nbt");
            if (nbtElement.isJsonObject()) {
                Tag nbtTag = nbtJsonParser.fromJson(nbtElement);
                if(nbtTag == null || !(nbtTag instanceof CompoundTag)) {
                    throw new IllegalArgumentException("Invalid NBT data for ItemStack");
                }
                stack.setTag((CompoundTag)nbtTag);
            } else {
                throw new IllegalArgumentException("Invalid NBT data for ItemStack");
            }
        }
        else {
            // Check if it has old parser data
            if(data.has("StoredEnchantments") || data.has("Potion")) {
                ItemData itemData = new ItemData();
                if(itemData.fromJson(json)) {
                    return itemData.getItemStack();
                } else {
                    throw new IllegalArgumentException("Invalid JSON element for ItemStack");
                }
            }
        }
        return stack;

        /*ItemData itemData = new ItemData();
        if(itemData.fromJson(json)) {
            return itemData.getItemStack();
        } else {
            throw new IllegalArgumentException("Invalid JSON element for ItemStack");
        }*/
    }
}
