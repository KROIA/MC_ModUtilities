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

public class CreativeItemsGenerator {
    public static List<ItemStack> generateAllCreativeItems(RegistryAccess registryAccess) {
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
