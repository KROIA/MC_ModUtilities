package net.kroia.modutilities.forge;

import net.kroia.modutilities.PlatformAbstraction;
import net.kroia.modutilities.UtilitiesPlatform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class UtilitiesPlatformForge implements PlatformAbstraction {
    @Override
    public ItemStack getItemStack(String itemID) {
        return new ItemStack(Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemID))));
    }

    @Override
    public String getItemID(Item item) {
        return Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(item)).toString();
    }

    @Override
    public ArrayList<ItemStack> getAllItems() {
        ArrayList<ItemStack> items = new ArrayList<>();
        ForgeRegistries.ITEMS.forEach(item -> items.add(new ItemStack(item)));

        /*
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            items.addAll(tab.getDisplayItems());
        }*/

        return items;
    }

    @Override
    public MinecraftServer getServer() {
        return net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
    }

    @Override
    public UtilitiesPlatform.Type getPlatformType() {
        return UtilitiesPlatform.Type.FORGE;
    }
}
