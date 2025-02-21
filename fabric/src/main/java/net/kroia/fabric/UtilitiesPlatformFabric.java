package net.kroia.fabric;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.PlatformAbstraction;
import net.kroia.modutilities.UtilitiesPlatform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;

public class UtilitiesPlatformFabric implements PlatformAbstraction {

    private static MinecraftServer minecraftServer;
    public static void setServer(MinecraftServer server) {
        minecraftServer = server;
        if(minecraftServer != null)
        {
            ModUtilitiesMod.LOGGER.info("[FabricSetup] SERVER INSTANCE SET");
        }
        else {
            ModUtilitiesMod.LOGGER.info("[FabricSetup] SERVER INSTANCE CLEARED");
        }
    }
    @Override
    public ItemStack getItemStack(String itemID) {
        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(itemID));
        return item != null ? new ItemStack(item) : ItemStack.EMPTY;
    }

    @Override
    public String getItemID(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    @Override
    public ArrayList<ItemStack> getAllItems() {
        ArrayList<ItemStack> items = new ArrayList<>();

        for (Item item : BuiltInRegistries.ITEM) {
            items.add(new ItemStack(item));
        }

        return items;
    }

    @Override
    public MinecraftServer getServer() {
        if (minecraftServer == null) {
            throw new IllegalStateException(ModUtilitiesMod.MOD_ID+" MinecraftServer is not yet initialized.");
        }
        return minecraftServer;
    }

    @Override
    public UtilitiesPlatform.Type getPlatformType()
    {
        return UtilitiesPlatform.Type.FABRIC;
    }
}
