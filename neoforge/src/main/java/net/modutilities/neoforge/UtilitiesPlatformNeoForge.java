package net.modutilities.neoforge;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.PlatformAbstraction;
import net.kroia.modutilities.UtilitiesPlatform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;

import java.util.HashMap;

public class UtilitiesPlatformNeoForge implements PlatformAbstraction {

    private static MinecraftServer minecraftServer;
    public static void setServer(MinecraftServer server) {
        minecraftServer = server;
        if(minecraftServer != null)
        {
            ModUtilitiesMod.LOGGER.info("[NeoForgeSetup] SERVER INSTANCE SET");
        }
        else {
            ModUtilitiesMod.LOGGER.info("[NeoForgeSetup] SERVER INSTANCE CLEARED");
        }
    }
    @Override
    public ItemStack getItemStack(String itemID) {
        String namespace = itemID.split(":")[0];
        String path = itemID.split(":")[1];
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
        return new ItemStack(item);
    }

    @Override
    public String getItemID(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    @Override
    public HashMap<String, ItemStack> getAllItems() {
        HashMap<String, ItemStack> itemsMap = new HashMap<>();

        for (Item item : BuiltInRegistries.ITEM) {
            itemsMap.put(getItemID(item), new ItemStack(item));
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
    public UtilitiesPlatform.Type getPlatformType() {
        return UtilitiesPlatform.Type.NEOFORGE;
    }
}
