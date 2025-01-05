package net.kroia.fabric;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.PlatformAbstraction;
import net.kroia.modutilities.UtilitiesPlatform;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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
        Registry<Item> itemRegistry = Registry.ITEM;
        Item item = itemRegistry.get(new ResourceLocation(itemID));
        return item != null ? new ItemStack(item) : ItemStack.EMPTY;
    }

    @Override
    public String getItemID(Item item) {
        Registry<Item> itemRegistry = Registry.ITEM;
        return itemRegistry.getKey(item).toString();
    }

    @Override
    public HashMap<String, ItemStack> getAllItems() {
        HashMap<String, ItemStack> itemsMap = new HashMap<>();
        Registry<Item> itemRegistry = Registry.ITEM;

        for (Item item : itemRegistry) {
            itemsMap.put(itemRegistry.getKey(item).toString(), new ItemStack(item));
        }

        return itemsMap;
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
