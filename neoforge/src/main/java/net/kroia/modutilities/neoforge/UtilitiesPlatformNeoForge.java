package net.kroia.modutilities.neoforge;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.PlatformAbstraction;
import net.kroia.modutilities.UtilitiesPlatform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

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
        if(itemID.indexOf(":") == -1) {
            itemID = "minecraft:"+itemID;
        }
        String[] parts = itemID.split(":", 2);
        String namespace = parts[0];
        String path = parts[1];
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
        if (item == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

    @Override
    public String getItemIDStr(Item item) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        if (key == null) {
            return "";
        }
        return key.toString();
    }

    @Override
    public ArrayList<ItemStack> getAllItems() {
        ArrayList<ItemStack> list = new ArrayList<>();

        for (Item item : BuiltInRegistries.ITEM) {
            list.add(new ItemStack(item));
        }

        return list;
    }

    @Override
    public MinecraftServer getServer() {
        return minecraftServer;
    }

    @Override
    public UtilitiesPlatform.Type getPlatformType() {
        return UtilitiesPlatform.Type.NEOFORGE;
    }
}
