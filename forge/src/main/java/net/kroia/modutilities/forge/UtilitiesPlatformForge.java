package net.kroia.modutilities.forge;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.PlatformAbstraction;
import net.kroia.modutilities.UtilitiesPlatform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;

public class UtilitiesPlatformForge implements PlatformAbstraction {

    private static MinecraftServer minecraftServer;

    public static void setServer(MinecraftServer server) {
        minecraftServer = server;
        if (minecraftServer != null) {
            ModUtilitiesMod.LOGGER.info("[ForgeSetup] SERVER INSTANCE SET");
        } else {
            ModUtilitiesMod.LOGGER.info("[ForgeSetup] SERVER INSTANCE CLEARED");
        }
    }

    @Override
    public ItemStack getItemStack(String itemID) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemID));
        if (item == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

    @Override
    public String getItemIDStr(Item item) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        if (key == null) {
            return "";
        }
        return key.toString();
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
        return minecraftServer;
    }

    @Override
    public UtilitiesPlatform.Type getPlatformType() {
        return UtilitiesPlatform.Type.FORGE;
    }
}
