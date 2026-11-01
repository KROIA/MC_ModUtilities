package net.kroia.modutilities.testing.tests;

import net.kroia.modutilities.UtilitiesPlatform;
import net.kroia.modutilities.testing.TestCategory;
import net.kroia.modutilities.testing.TestResult;
import net.kroia.modutilities.testing.TestSuite;
import net.kroia.modutilities.testing.categories.ModUtilitiesTestCategories;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

import net.kroia.modutilities.ModUtilitiesMod;

/**
 * Tests whether the Minecraft CreativeModeTab API can be used to get items
 * grouped by tab from common code.
 */
public class CreativeTabTests extends TestSuite {

    @Override
    public TestCategory getCategory() {
        return ModUtilitiesTestCategories.UTILITIES;
    }

    @Override
    public void registerTests() {
        addTest("creative_tab_registry_accessible", this::testCreativeTabRegistryAccessible);
        addTest("creative_tab_rebuild_contents", this::testCreativeTabRebuildContents);
        addTest("creative_tab_has_display_items", this::testCreativeTabHasDisplayItems);
        addTest("creative_tab_includes_variants", this::testCreativeTabIncludesVariants);
        addTest("creative_tab_items_grouped_by_tab", this::testCreativeTabItemsGroupedByTab);
        addTest("creative_tab_total_vs_registry", this::testCreativeTabTotalVsRegistry);
    }

    // ========================================================================
    // Helper: rebuild tab contents, returns null on success or a failure result
    // ========================================================================

    private TestResult rebuildTabContents() {
        RegistryAccess registryAccess = UtilitiesPlatform.getRegistryAccess();
        if (registryAccess == null) {
            return fail("No registry access available");
        }
        try {
            CreativeModeTabs.tryRebuildTabContents(FeatureFlags.DEFAULT_FLAGS, true, registryAccess);
        } catch (Exception e) {
            return fail("tryRebuildTabContents threw: " + e.getMessage());
        }
        return null; // success
    }

    // ========================================================================
    // Tests
    // ========================================================================

    private TestResult testCreativeTabRegistryAccessible() {
        int count = 0;
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            count++;
        }
        if (count > 0) {
            return pass("CREATIVE_MODE_TAB registry accessible with " + count + " tabs");
        }
        return fail("CREATIVE_MODE_TAB registry has 0 tabs");
    }

    private TestResult testCreativeTabRebuildContents() {
        RegistryAccess registryAccess = UtilitiesPlatform.getRegistryAccess();
        if (registryAccess == null) {
            return fail("No registry access available");
        }
        try {
            CreativeModeTabs.tryRebuildTabContents(FeatureFlags.DEFAULT_FLAGS, true, registryAccess);
        } catch (Exception e) {
            return fail("tryRebuildTabContents threw: " + e.getMessage());
        }
        return pass("tryRebuildTabContents completed without exception");
    }

    private TestResult testCreativeTabHasDisplayItems() {
        TestResult rebuildResult = rebuildTabContents();
        if (rebuildResult != null) return rebuildResult;

        int totalTabCount = 0;
        int totalItemCount = 0;
        boolean anyTabHasItems = false;

        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            totalTabCount++;
            Collection<ItemStack> items = tab.getDisplayItems();
            int itemCount = items.size();
            totalItemCount += itemCount;
            if (itemCount > 0) {
                anyTabHasItems = true;
            }
        }

        if (anyTabHasItems) {
            return pass("At least one tab has items. Total tabs: " + totalTabCount + ", total items: " + totalItemCount);
        }
        return fail("No tab has any display items. Total tabs: " + totalTabCount);
    }

    private TestResult testCreativeTabIncludesVariants() {
        TestResult rebuildResult = rebuildTabContents();
        if (rebuildResult != null) return rebuildResult;

        int totalPotions = 0;
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            int potionCount = 0;
            for (ItemStack stack : tab.getDisplayItems()) {
                if (stack.is(Items.POTION)) {
                    potionCount++;
                }
            }
            if (potionCount > 0) {
                ModUtilitiesMod.LOGGER.info("[CreativeTabTest] Tab '{}' has {} potions, {} total items",
                        tab.getDisplayName().getString(), potionCount, tab.getDisplayItems().size());
            }
            totalPotions += potionCount;
            if (potionCount > 1) {
                return pass("Tab '" + tab.getDisplayName().getString()
                        + "' contains " + potionCount + " potion variants");
            }
        }
        return fail("No tab has >1 potion variant. Total potions across all tabs: " + totalPotions);
    }

    private TestResult testCreativeTabItemsGroupedByTab() {
        TestResult rebuildResult = rebuildTabContents();
        if (rebuildResult != null) return rebuildResult;

        LinkedHashMap<String, List<ItemStack>> groupedItems = new LinkedHashMap<>();
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            String tabName = tab.getDisplayName().getString();
            List<ItemStack> items = new ArrayList<>(tab.getDisplayItems());
            groupedItems.put(tabName, items);
            ModUtilitiesMod.LOGGER.info("[CreativeTabTest] Tab '{}': {} items", tabName, items.size());
        }

        if (groupedItems.size() <= 1) {
            return fail("Expected more than 1 tab entry, got " + groupedItems.size());
        }

        // Remove special UI tabs that don't contain items (Saved Hotbars, Survival Inventory)
        groupedItems.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        if (groupedItems.isEmpty()) {
            return fail("All tabs are empty after filtering");
        }

        List<String> tabNames = new ArrayList<>(groupedItems.keySet());
        int reportCount = Math.min(3, tabNames.size());
        StringBuilder sb = new StringBuilder();
        sb.append("Map has ").append(groupedItems.size()).append(" entries. First tabs: ");
        for (int i = 0; i < reportCount; i++) {
            if (i > 0) sb.append(", ");
            sb.append(tabNames.get(i));
        }
        return pass(sb.toString());
    }

    private TestResult testCreativeTabTotalVsRegistry() {
        TestResult rebuildResult = rebuildTabContents();
        if (rebuildResult != null) return rebuildResult;

        Set<net.minecraft.world.item.Item> uniqueTabItems = new HashSet<>();
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            for (ItemStack stack : tab.getDisplayItems()) {
                uniqueTabItems.add(stack.getItem());
            }
        }

        int registrySize = 0;
        for (net.minecraft.world.item.Item ignored : BuiltInRegistries.ITEM) {
            registrySize++;
        }

        int tabTotal = uniqueTabItems.size();
        int totalStacks = 0;
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            totalStacks += tab.getDisplayItems().size();
        }
        ModUtilitiesMod.LOGGER.info("[CreativeTabTest] Unique items in tabs: {}, total stacks: {}, registry size: {}",
                tabTotal, totalStacks, registrySize);
        // Tabs won't contain every registry item (admin-only items like structure_void,
        // jigsaw aren't in any tab). Total stacks should exceed registry due to variants.
        if (totalStacks > registrySize) {
            return pass("Total stacks (" + totalStacks + ") > registry size (" + registrySize
                    + "), unique items: " + tabTotal);
        }
        return fail("Total stacks (" + totalStacks + ") <= registry size (" + registrySize
                + "), unique items: " + tabTotal);
    }
}
