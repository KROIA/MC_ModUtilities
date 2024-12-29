package net.kroia.modutilities;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

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
        ResourceLocation resourceLocation = new ResourceLocation(itemId); // "minecraft:diamond"
        Item item = ForgeRegistries.ITEMS.getValue(resourceLocation); // Get the item from the registry

        if (item != null) {
            return new ItemStack(item, amount); // Create an ItemStack with the specified amount
        }

        return ItemStack.EMPTY; // Return an empty stack if the item is not found
    }
    public static String getNormalizedItemID(String maybeNotCompleteItemID)
    {
        ItemStack itemStack = createItemStackFromId(maybeNotCompleteItemID,1);
        if(itemStack == null) {
            return null;
        }
        if(itemStack.getItem() == Items.AIR) {
            return null;
        }
        // Get the item's ResourceLocation
        return getItemID(itemStack.getItem());
    }
    public static String getItemID(Item item)
    {
        ResourceLocation itemLocation = ForgeRegistries.ITEMS.getKey(item);
        return itemLocation.toString();
    }

    public static ArrayList<String> getAllItemIDs()
    {
        ArrayList<String> itemIDs = new ArrayList<>();
        for(Item item : ForgeRegistries.ITEMS)
        {
            itemIDs.add(getItemID(item));
        }
        return itemIDs;
    }
    public static ArrayList<ItemStack> getAllItems()
    {
        ArrayList<ItemStack> items = new ArrayList<>();
        for(Item item : ForgeRegistries.ITEMS)
        {
            items.add(new ItemStack(item));
        }
        return items;
    }

    public static ArrayList<String> getAllItemIDs(String tag)
    {
        ArrayList<String> itemIDs = new ArrayList<>();
        for(Item item : ForgeRegistries.ITEMS)
        {
            if(isInTag(item, tag))
            {
                itemIDs.add(getItemID(item));
            }
        }
        return itemIDs;
    }
    public static ArrayList<String> getAllItemIDs(ArrayList<String> tags)
    {
        ArrayList<String> itemIDs = new ArrayList<>();
        HashMap<String, TagKey<Item>> tagMap = new HashMap<>();
        for(String tag : tags)
        {
            tagMap.put("forge:"+tag, TagKey.create(Registries.ITEM, new ResourceLocation("forge:"+tag)));
            tagMap.put("minecraft:"+tag, TagKey.create(Registries.ITEM, new ResourceLocation("minecraft:"+tag)));
        }
        for(Item item : ForgeRegistries.ITEMS)
        {
            if(item.builtInRegistryHolder().tags().anyMatch(tagMap::containsValue))
            {
                itemIDs.add(getItemID(item));
            }
            if(getItemID(item).compareTo("minecraft:diamond") == 0)
            {
                System.out.println("Item is in tag: "+item.builtInRegistryHolder().tags().anyMatch(tagMap::containsValue));
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
