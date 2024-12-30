package net.kroia.modutilities;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;

public class UtilitiesPlatform {
    public enum Type {
        FABRIC,
        FORGE,
        QUILT
    }
    private static PlatformAbstraction platform;

    public static PlatformAbstraction getPlatform() {
        if (platform == null) {
            throw new IllegalStateException("UtilitiesPlatform not set");
        }
        return platform;
    }

    public static void setPlatform(PlatformAbstraction platform) {
        UtilitiesPlatform.platform = platform;
    }


    public static ItemStack getItemStack(String itemID) {
        return getPlatform().getItemStack(itemID);
    }

    public static String getItemID(Item item) {
        return getPlatform().getItemID(item);
    }

    public static HashMap<String, ItemStack> getAllItems() {
        return getPlatform().getAllItems();
    }
    public static MinecraftServer getServer() {
        return getPlatform().getServer();
    }
    public static Type getPlatformType() {
        return getPlatform().getPlatformType();
    }

    public static boolean isForge() {
        return getPlatformType() == Type.FORGE;
    }
    public static boolean isFabric() {
        return getPlatformType() == Type.FABRIC;
    }
    public static boolean isQuilt() {
        return getPlatformType() == Type.QUILT;
    }
}
