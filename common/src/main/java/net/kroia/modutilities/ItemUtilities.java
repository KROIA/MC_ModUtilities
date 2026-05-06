package net.kroia.modutilities;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ItemUtilities {

    private static class ItemCache
    {
        private List<ItemStack> creativeItemCache;

        public List<ItemStack> getCreativeItemCache()
        {
            if(creativeItemCache == null)
            {
                creativeItemCache = CreativeItemsGenerator.generateAllCreativeItems(UtilitiesPlatform.getRegistryAccess());
            }
            return creativeItemCache;
        }

        public void invalidate()
        {
            creativeItemCache = null;
        }
    }
    private static final ItemCache itemCache = new ItemCache();

    /**
     * Invalidates the cached creative items list. Call this when the world changes (load/unload),
     * resource packs reload, or registries change to ensure subsequent searches reflect current state.
     */
    public static void invalidateCreativeItemCache()
    {
        itemCache.invalidate();
    }

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
        if(itemStack.isEmpty()) {
            return null;
        }
        if(itemStack.getItem() == Items.AIR) {
            return null;
        }
        // Get the item's ResourceLocation
        return getItemIDStr(itemStack.getItem());
    }
    public static String getItemName(Item item)
    {
        return item.toString();
    }
    public static String getItemName(String itemID)
    {
        return getItemName(createItemStackFromId(itemID).getItem());
    }
    public static String getItemIDStr(Item item)
    {
        return UtilitiesPlatform.getItemIDStr(item);
    }


    public static ArrayList<String> getAllItemIDStrs()
    {
        ArrayList<ItemStack> items = UtilitiesPlatform.getAllItems();
        HashMap<String, ItemStack> itemTable = new HashMap<>();
        for(ItemStack stack : items)
        {
            itemTable.put(getItemIDStr(stack.getItem()), stack);
        }
        return new ArrayList<>(itemTable.keySet());
    }
    public static ArrayList<ItemStack> getAllItems()
    {
        return UtilitiesPlatform.getAllItems();
    }

    public static ArrayList<String> getAllItemIDStrs(String tag)
    {
        ArrayList<String> itemIDs = new ArrayList<>();
        ArrayList<ItemStack> items = UtilitiesPlatform.getAllItems();
        for(ItemStack stack : items)
        {
            if(isInTag(stack.getItem(), tag))
            {
                itemIDs.add(getItemIDStr(stack.getItem()));
            }
        }
        return itemIDs;
    }
    public static ArrayList<ItemStack> getAllItems(String tag)
    {
        ArrayList<ItemStack> items = new ArrayList<>();
        ArrayList<ItemStack> itemTable = UtilitiesPlatform.getAllItems();
        for(ItemStack stack : itemTable)
        {
            if(isInTag(stack.getItem(), tag))
            {
                items.add(stack);
            }
        }
        return items;
    }
    public static ArrayList<String> getAllItemIDStrs(ArrayList<String> tags, ArrayList<String> containsInID)
    {
        ArrayList<String> itemIDs = new ArrayList<>();
        HashMap<String, TagKey<Item>> tagMap = new HashMap<>();
        ArrayList<ItemStack> itemTable = UtilitiesPlatform.getAllItems();
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
            tagMap.put(modTag+tag, TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ModUtilitiesMod.MOD_ID, modTag+tag)));
            tagMap.put("minecraft:"+tag, TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ModUtilitiesMod.MOD_ID,"minecraft:"+tag)));
        }
        for(ItemStack stack : itemTable)
        {
            Item item = stack.getItem();
            String itemName = getItemIDStr(item);
            if(item.builtInRegistryHolder().tags().anyMatch(tagMap::containsValue) ||
                    containsInID.contains(itemName))
            {
                itemIDs.add(itemName);
            }
        }
        return itemIDs;
    }
    public static ArrayList<ItemStack> getAllItems(ArrayList<String> tags, ArrayList<String> containsInID)
    {
        ArrayList<ItemStack> items = new ArrayList<>();
        HashMap<String, TagKey<Item>> tagMap = new HashMap<>();
        ArrayList<ItemStack> itemTable = UtilitiesPlatform.getAllItems();
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
            tagMap.put(modTag+tag, TagKey.create(Registries.ITEM, ResourceLocation.parse(modTag+tag)));
            tagMap.put("minecraft:"+tag, TagKey.create(Registries.ITEM, ResourceLocation.parse("minecraft:"+tag)));
        }
        for(ItemStack stack : itemTable)
        {
            Item item = stack.getItem();
            String itemName = getItemIDStr(item);
            if(item.builtInRegistryHolder().tags().anyMatch(tagMap::containsValue) ||
                    containsInID.contains(itemName))
            {
                items.add(stack);
            }
        }
        return items;
    }
    private static boolean isInTag(Item item, String tagId) {
        // Create a TagKey for the specified tag
        TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ModUtilitiesMod.MOD_ID,tagId));

        // Check if the item is in the tag
        return item.builtInRegistryHolder().is(tag);
    }


    /**
     *  Searches for items in the creative inventory based on the provided search text.
     * @param searchText The text to search for in item names.
     * @return A list of ItemStacks that match the search criteria.
     */
    public static List<ItemStack> getSearchCreativeItems(String searchText)
    {
        //CreativeModeTabs.tryRebuildTabContents(FeatureFlags.DEFAULT_FLAGS, true, UtilitiesPlatform.getRegistryAccess());
        //var searchTree = Minecraft.getInstance().getSearchTree(SearchRegistry.CREATIVE_NAMES);
        //return searchTree.search(searchText.toLowerCase(Locale.ROOT));
        List<ItemStack> creativeItems = itemCache.getCreativeItemCache();
        List<ItemStack> searchResults = new ArrayList<>();
        String lowerSearchText = searchText.toLowerCase(Locale.ROOT);
        for (ItemStack itemStack : creativeItems) {
            String itemName = itemStack.getHoverName().getString().toLowerCase(Locale.ROOT);
            if (itemName.contains(lowerSearchText)) {
                searchResults.add(itemStack);
            }
        }
        return searchResults;
    }


    /**
     * Searches for items in the creative inventory based on the provided search text and a specific search key.
     * @param searchText The text to search for in item names.
     * @param key The search key to use for the search.
     * @return A list of ItemStacks that match the search criteria.
     */
    /*public static List<ItemStack> getSearchItems(String searchText, SearchRegistry.Key<ItemStack> key)
    {
        CreativeModeTabs.tryRebuildTabContents(FeatureFlags.DEFAULT_FLAGS, true, UtilitiesPlatform.getRegistryAccess());
        var searchTree = Minecraft.getInstance().getSearchTree(key);
        return searchTree.search(searchText.toLowerCase(Locale.ROOT));
    }*/


    /**
     * Extracts the search text from an item ID.
     * @param itemID The item ID to extract the search text from.
     * @return The search text extracted from the item ID, or an empty string if the item ID is null or empty.
     */
    public static String getSearchTextFromItemID(String itemID)
    {
        if(itemID == null || itemID.isEmpty())
            return "";
        String[] parts = itemID.split(":");
        String secondPart = itemID;
        if(parts.length > 1)
        {
            secondPart = parts[parts.length-1];
        }
        // Replace "_" with " " in the second part of the ID
        secondPart = secondPart.replace("_", " ");
        return secondPart; // Return the part after the colon
    }



    /**
     * Drops the specified item stack at the player's position.
     * @param player the player who will drop the item
     * @param stack the item stack to be dropped
     */
    public static void dropItemAtPlayer(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return;
        }

        Level level = player.level();
        Vec3 position = player.position().add(0, 1, 0); // Drop item slightly above the player

        ItemEntity itemEntity = new ItemEntity(level, position.x, position.y, position.z, stack);
        itemEntity.setDefaultPickUpDelay(); // Set default pickup delay
        level.addFreshEntity(itemEntity); // Add the item entity to the world
    }
}
