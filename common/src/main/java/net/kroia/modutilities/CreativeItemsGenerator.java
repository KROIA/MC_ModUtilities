package net.kroia.modutilities;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * Enumerates every {@link ItemStack} that should be visible in the creative inventory.
 * <p>
 * Walks the item, potion and enchantment registries to produce stacks for:
 * <ul>
 *   <li>every regular item (excluding admin-only items such as command blocks or the debug stick),</li>
 *   <li>every potion variant (regular, splash, lingering and tipped arrows),</li>
 *   <li>every enchantment level for every enchantment, as enchanted books.</li>
 * </ul>
 *
 * @apiNote The generated list is typically cached by {@link ItemUtilities}; call
 *          {@link ItemUtilities#invalidateCreativeItemCache()} when the world or registries change.
 */
public class CreativeItemsGenerator {
    /**
     * Builds the full list of creative-mode item stacks for the current registries.
     *
     * @param registryAccess registry access used to enumerate enchantments
     * @return a list containing every stack that should appear in the creative inventory
     */
    public static List<ItemStack> generateAllCreativeItems(RegistryAccess registryAccess) {
        if (registryAccess == null) {
            return new ArrayList<>();
        }
        List<ItemStack> results = new ArrayList<>();

        // 1. Add all base items
        for (Item item : BuiltInRegistries.ITEM) {
            //  // Skip items that are not creative tab items or are not suitable for general use
            if (    item == Items.AIR ||
                    item == Items.BARRIER ||
                    item == Items.DEBUG_STICK ||
                    item == Items.COMMAND_BLOCK) {
                continue; // Skip items that are not suitable for general use
            }

            // Skip Enchantment book, as it will be handled separately
            if (item == Items.ENCHANTED_BOOK) {
                continue; // Skip enchanted book item
            }

            // Skip tipped arrow and potions as they will be handled separately
            if (    item == Items.TIPPED_ARROW ||
                    item == Items.POTION ||
                    item == Items.SPLASH_POTION ||
                    item == Items.LINGERING_POTION) {
                continue; // Skip potion-related items
            }

            results.add(new ItemStack(item));
        }

        // 2. Add all potion variants (Potion, Splash, Lingering, Tipped Arrows)
        for (Potion potion : BuiltInRegistries.POTION) {
            results.add(PotionContents.createItemStack(Items.POTION, Holder.direct(potion)));
            results.add(PotionContents.createItemStack(Items.SPLASH_POTION, Holder.direct(potion)));
            results.add(PotionContents.createItemStack(Items.LINGERING_POTION, Holder.direct(potion)));
            results.add(PotionContents.createItemStack(Items.TIPPED_ARROW, Holder.direct(potion)));
        }

        // 3. Add all enchanted books for every enchantment and level
        for (Holder<Enchantment> enchantment : registryAccess.registryOrThrow(Registries.ENCHANTMENT).holders().toList()) {
            for (int lvl = enchantment.value().getMinLevel(); lvl <= enchantment.value().getMaxLevel(); lvl++) {
                results.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, lvl)));
            }
        }
        return results;
    }
}
