package net.kroia.modutilities;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;

public class ItemUtilities {
    public static ItemStack createItemStackFromId(String itemId)
    {
        return createItemStackFromId(itemId, 1);
    }
    public static ItemStack createItemStackFromId(String itemId, int amount)
    {
        if(itemId == null) {
            return ItemStack.EMPTY;
        }
        if(itemId.indexOf(":") == -1) {
            itemId = "minecraft:"+itemId;
        }

        ItemStack itemStack = UtilitiesPlatform.getItemStack(itemId); // Get the item from the item ID

        if (itemStack != null) {
            itemStack.setCount(amount); // Set the item stack's count to the specified amount
            return itemStack; // Return the item stack
        }

        return ItemStack.EMPTY; // Return an empty stack if the item is not found
    }
    public static String getNormalizedItemID(String maybeNotCompleteItemID)
    {
        if(maybeNotCompleteItemID == null) {
            return null;
        }
        ItemStack itemStack = createItemStackFromId(maybeNotCompleteItemID,1);
        if(itemStack == itemStack.EMPTY) {
            return null;
        }
        if(itemStack.getItem() == Items.AIR) {
            return null;
        }
        // Get the item's ResourceLocation
        return getItemID(itemStack.getItem());
    }
    public static String getItemName(Item item)
    {
        return item.toString();
    }
    public static String getItemName(String itemID)
    {
        return getItemName(createItemStackFromId(itemID).getItem());
    }
    public static String getItemID(Item item)
    {
        return UtilitiesPlatform.getItemID(item);
    }

    public static ArrayList<String> getAllItemIDs()
    {
        ArrayList<String> itemIDs = new ArrayList<>();
        HashMap<String, ItemStack> itemTable = UtilitiesPlatform.getAllItems();
        for(String itemID : itemTable.keySet())
        {
            itemIDs.add(itemID);
        }
        return itemIDs;
    }
    public static ArrayList<ItemStack> getAllItems()
    {
        ArrayList<ItemStack> items = new ArrayList<>();
        HashMap<String, ItemStack> itemTable = UtilitiesPlatform.getAllItems();
        for(ItemStack stack : itemTable.values())
        {
            items.add(stack);
        }
        return items;
    }

    public static ArrayList<String> getAllItemIDs(String tag)
    {
        ArrayList<String> itemIDs = new ArrayList<>();
        HashMap<String, ItemStack> itemTable = UtilitiesPlatform.getAllItems();
        for(var entry : itemTable.entrySet())
        {
            if(isInTag(entry.getValue().getItem(), tag))
            {
                itemIDs.add(entry.getKey());
            }
        }
        return itemIDs;
    }
    public static ArrayList<String> getAllItemIDs(ArrayList<String> tags, ArrayList<String> containsInID)
    {
        ArrayList<String> itemIDs = new ArrayList<>();
        HashMap<String, TagKey<Item>> tagMap = new HashMap<>();
        HashMap<String, ItemStack> itemTable = UtilitiesPlatform.getAllItems();
        String modTag = "";
        switch(UtilitiesPlatform.getPlatformType())
        {
            case FABRIC:
                modTag = "c:";
                break;
            case FORGE:
                modTag = "forge:";
                break;
            case QUILT:
                modTag = "c:";
                break;
        }
        for(String tag : tags)
        {
            tagMap.put(modTag+tag, TagKey.create(Registries.ITEM, new ResourceLocation(modTag+tag)));
            tagMap.put("minecraft:"+tag, TagKey.create(Registries.ITEM, new ResourceLocation("minecraft:"+tag)));
        }
        for(ItemStack stack : itemTable.values())
        {
            Item item = stack.getItem();
            String itemName = getItemID(item);
            if(item.builtInRegistryHolder().tags().anyMatch(tagMap::containsValue) ||
                    containsInID.contains(itemName))
            {
                itemIDs.add(itemName);
            }
        }
        return itemIDs;
    }
    private static boolean isInTag(Item item, String tagId) {
        // Create a TagKey for the specified tag
        TagKey<Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation(tagId));

        // Check if the item is in the tag
        return item.builtInRegistryHolder().is(tag);
    }
}
