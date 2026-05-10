package net.kroia.modutilities;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * High-level helper for working with Minecraft {@link Item}s and {@link ItemStack}s.
 * <p>
 * Provides convenience methods for resolving items from string IDs, enumerating items
 * (optionally filtered by tag), querying the creative inventory cache, performing search-by-name
 * lookups, and dropping items into the world.
 * <p>
 * Many methods delegate to {@link UtilitiesPlatform} which abstracts platform-specific
 * registry access between Fabric, Forge, NeoForge and Quilt.
 *
 * @apiNote
 * The creative-item cache used by {@link #getSearchCreativeItems(String)} is populated lazily on
 * first access and is otherwise never invalidated automatically. Call
 * {@link #invalidateCreativeItemCache()} when the world is loaded or unloaded, when registries
 * change, or when resource packs reload, so subsequent searches reflect the current state.
 */
public class ItemUtilities {

    private static class ItemCache
    {
        private List<ItemStack> creativeItemCache;
        private List<ItemStack> creativeTabAllItems;
        private LinkedHashMap<String, List<ItemStack>> creativeTabCategories;

        public List<ItemStack> getCreativeItemCache()
        {
            if(creativeItemCache == null)
            {
                creativeItemCache = CreativeItemsGenerator.generateAllCreativeItems(UtilitiesPlatform.getRegistryAccess());
            }
            return creativeItemCache;
        }

        public List<ItemStack> getCreativeTabAllItems()
        {
            if(creativeTabAllItems == null)
            {
                rebuildCreativeTabCache();
            }
            return creativeTabAllItems;
        }

        public LinkedHashMap<String, List<ItemStack>> getCreativeTabCategories()
        {
            if(creativeTabCategories == null)
            {
                rebuildCreativeTabCache();
            }
            return creativeTabCategories;
        }

        private void rebuildCreativeTabCache()
        {
            RegistryAccess registryAccess = UtilitiesPlatform.getRegistryAccess();
            if(registryAccess == null)
            {
                creativeTabAllItems = new ArrayList<>();
                creativeTabCategories = new LinkedHashMap<>();
                return;
            }
            CreativeModeTabs.tryRebuildTabContents(FeatureFlags.DEFAULT_FLAGS, true, registryAccess);

            creativeTabCategories = new LinkedHashMap<>();
            List<ItemStack> searchTabItems = null;

            for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
                Collection<ItemStack> displayItems = tab.getDisplayItems();
                if (displayItems.isEmpty()) continue;

                String tabName = tab.getDisplayName().getString();
                creativeTabCategories.put(tabName, new ArrayList<>(displayItems));

                if (tab.getType() == CreativeModeTab.Type.SEARCH) {
                    searchTabItems = new ArrayList<>(displayItems);
                }
            }

            if (searchTabItems != null) {
                creativeTabAllItems = searchTabItems;
            } else {
                Set<ItemStack> seen = new LinkedHashSet<>();
                for (List<ItemStack> items : creativeTabCategories.values()) {
                    seen.addAll(items);
                }
                creativeTabAllItems = new ArrayList<>(seen);
            }
        }

        public void invalidate()
        {
            creativeItemCache = null;
            creativeTabAllItems = null;
            creativeTabCategories = null;
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

    /**
     * Creates a single-item {@link ItemStack} from a string item ID.
     *
     * @param itemId the item ID to resolve, with or without a namespace
     *               (e.g. {@code "minecraft:stone"} or {@code "stone"}); {@code null} returns {@link ItemStack#EMPTY}
     * @return an {@link ItemStack} of count 1, or {@link ItemStack#EMPTY} if the ID could not be resolved
     */
    public static ItemStack createItemStackFromId(String itemId)
    {
        return createItemStackFromId(itemId, 1);
    }
    /**
     * Creates an {@link ItemStack} from a string item ID with the specified count.
     * <p>
     * If {@code itemId} contains no {@code ':'} separator, the {@code "minecraft:"} namespace
     * is automatically prepended.
     *
     * @param itemId the item ID to resolve; {@code null} returns {@link ItemStack#EMPTY}
     * @param amount the desired stack size
     * @return an {@link ItemStack} of the given count, or {@link ItemStack#EMPTY} if the ID could not be resolved
     */
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
    /**
     * Normalizes a possibly partial item ID into the canonical {@code namespace:path} form.
     * <p>
     * The given ID is resolved through {@link #createItemStackFromId(String)} and the resulting
     * item's full registry ID string is returned.
     *
     * @param maybeNotCompleteItemID a partial or complete item ID; may be {@code null}
     * @return the canonical item ID, or {@code null} if the input was {@code null},
     *         the resolved stack is empty, or the item is air
     * @apiNote Empty results are detected via {@link ItemStack#isEmpty()}, so unknown item IDs
     *          (which resolve to {@link ItemStack#EMPTY}) yield {@code null}.
     */
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
    /**
     * Returns a debug-style name for the given item, derived from {@link Item#toString()}.
     *
     * @param item the item to name
     * @return the item's string representation
     */
    public static String getItemName(Item item)
    {
        return item.toString();
    }
    /**
     * Returns a debug-style name for the item identified by the given ID.
     *
     * @param itemID the item ID to resolve
     * @return the resolved item's string representation
     */
    public static String getItemName(String itemID)
    {
        return getItemName(createItemStackFromId(itemID).getItem());
    }
    /**
     * Returns the canonical {@code namespace:path} registry ID of the given item.
     *
     * @param item the item to look up
     * @return the registry ID string for the item
     */
    public static String getItemIDStr(Item item)
    {
        return UtilitiesPlatform.getItemIDStr(item);
    }


    // =========================================================================
    // Client-side methods — use CreativeModeTab API, include item variants
    // =========================================================================

    /**
     * Returns all creative inventory items including variants (potions, enchanted books,
     * tipped arrows, etc.) as seen in the creative mode inventory.
     * <p>
     * Client-side only — uses {@link CreativeModeTabs#tryRebuildTabContents} to populate
     * tab contents. Results are cached and invalidated by {@link #invalidateCreativeItemCache()}.
     *
     * @return a list of all creative inventory item stacks, or an empty list if no registry access is available
     */
    public static List<ItemStack> getAllItems()
    {
        return itemCache.getCreativeTabAllItems();
    }

    /**
     * Returns all creative inventory items grouped by their creative tab category.
     * <p>
     * Keys are tab display names (e.g. "Building Blocks", "Combat", "Food &amp; Drinks").
     * Empty special tabs (Saved Hotbars, Survival Inventory) are excluded.
     * Client-side only.
     *
     * @return an ordered map of tab name to item stacks, or an empty map if no registry access is available
     */
    public static LinkedHashMap<String, List<ItemStack>> getItemsByCategory()
    {
        return itemCache.getCreativeTabCategories();
    }

    // =========================================================================
    // Server-side methods — use item registry, one stack per registered item
    // =========================================================================

    /**
     * Returns the registry IDs of all items known to the platform.
     * Works on both client and server. Returns one entry per registered item type (no variants).
     *
     * @return a list of unique item ID strings
     */
    public static ArrayList<String> getAllItemIDStrsServerSide()
    {
        ArrayList<ItemStack> items = UtilitiesPlatform.getAllItems();
        HashMap<String, ItemStack> itemTable = new HashMap<>();
        for(ItemStack stack : items)
        {
            itemTable.put(getItemIDStr(stack.getItem()), stack);
        }
        return new ArrayList<>(itemTable.keySet());
    }

    /**
     * Returns all items known to the platform as {@link ItemStack}s.
     * Works on both client and server. Returns one stack per registered item type (no variants).
     *
     * @return a list of item stacks, one per registered item
     */
    public static ArrayList<ItemStack> getAllItemsServerSide()
    {
        return UtilitiesPlatform.getAllItems();
    }

    /**
     * Returns the IDs of all items belonging to the given item tag.
     * Works on both client and server.
     *
     * @param tag the tag identifier including namespace (e.g. {@code "minecraft:logs"}, {@code "c:ingots"})
     * @return a list of matching item ID strings
     */
    public static ArrayList<String> getAllItemIDStrsServerSide(String tag)
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

    /**
     * Returns all item stacks belonging to the given item tag.
     * Works on both client and server.
     *
     * @param tag the tag identifier including namespace (e.g. {@code "minecraft:logs"}, {@code "c:ingots"})
     * @return a list of matching item stacks
     */
    public static ArrayList<ItemStack> getAllItemsServerSide(String tag)
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

    /**
     * Returns the IDs of all items matching any of the provided tags or whose ID is contained
     * in {@code containsInID}.
     * Works on both client and server.
     * <p>
     * Tags are looked up under both the platform-specific common namespace
     * ({@code c:} on Fabric/Quilt, {@code forge:} on Forge) and the {@code minecraft:} namespace.
     *
     * @param tags         tag paths (without namespace) to match against
     * @param containsInID exact item ID strings to include unconditionally
     * @return a list of matching item ID strings
     */
    public static ArrayList<String> getAllItemIDStrsServerSide(ArrayList<String> tags, ArrayList<String> containsInID)
    {
        ArrayList<String> itemIDs = new ArrayList<>();
        HashMap<String, TagKey<Item>> tagMap = new HashMap<>();
        ArrayList<ItemStack> itemTable = UtilitiesPlatform.getAllItems();
        String modTag = "";
        switch(UtilitiesPlatform.getPlatformType())
        {
            case FABRIC:
            case NEOFORGE:
            case QUILT:
                modTag = "c:";
                break;
            case FORGE:
                modTag = "forge:";
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
                itemIDs.add(itemName);
            }
        }
        return itemIDs;
    }

    /**
     * Returns the item stacks of all items matching any of the provided tags or whose ID is
     * contained in {@code containsInID}.
     * Works on both client and server.
     * <p>
     * Tags are looked up under both the platform-specific common namespace
     * ({@code c:} on Fabric/Quilt, {@code forge:} on Forge) and the {@code minecraft:} namespace.
     *
     * @param tags         tag paths (without namespace) to match against
     * @param containsInID exact item ID strings to include unconditionally
     * @return a list of matching item stacks
     */
    public static ArrayList<ItemStack> getAllItemsServerSide(ArrayList<String> tags, ArrayList<String> containsInID)
    {
        ArrayList<ItemStack> items = new ArrayList<>();
        HashMap<String, TagKey<Item>> tagMap = new HashMap<>();
        ArrayList<ItemStack> itemTable = UtilitiesPlatform.getAllItems();
        String modTag = "";
        switch(UtilitiesPlatform.getPlatformType())
        {
            case FABRIC:
            case NEOFORGE:
            case QUILT:
                modTag = "c:";
                break;
            case FORGE:
                modTag = "forge:";
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
        TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(tagId));
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
