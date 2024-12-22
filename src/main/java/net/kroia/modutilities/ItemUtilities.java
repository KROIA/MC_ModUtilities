package net.kroia.modutilities;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

public class ItemUtilities {
    public static ItemStack createItemStackFromId(String itemId)
    {
        return createItemStackFromId(itemId, 1);
    }
    public static ItemStack createItemStackFromId(String itemId, int amount)
    {
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
        ResourceLocation itemLocation = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
        return itemLocation.toString();
    }
}
