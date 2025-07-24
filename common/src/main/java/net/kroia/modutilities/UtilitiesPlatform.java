package net.kroia.modutilities;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;

public class UtilitiesPlatform {
    public enum Type {
        FABRIC,
        FORGE,
        QUILT,
        NEOFORGE
    }
    private static PlatformAbstraction platform;

    public static PlatformAbstraction getPlatform() {
        if (platform == null) {
            throw new IllegalStateException(ModUtilitiesMod.MOD_ID+" Platform not set!");
        }
        return platform;
    }

    public static boolean isClient()
    {
        return Platform.getEnvironment() == Env.CLIENT;
    }
    public static boolean isServer()
    {
        return Platform.getEnvironment() == Env.SERVER;
    }

    public static RegistryAccess getRegistryAccess() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            return mc.level.registryAccess();
        } else if (mc.player != null) {
            return mc.player.connection.registryAccess();
        } else {
            return (RegistryAccess) BuiltInRegistries.REGISTRY.asLookup(); // fallback read-only registry
        }
    }

    public static void setPlatform(PlatformAbstraction platform) {
        UtilitiesPlatform.platform = platform;
        ModUtilitiesMod.LOGGER.info("UtilitiesPlatform set to: " + platform.getPlatformType().name());
    }


    public static ItemStack getItemStack(String itemID) {
        return getPlatform().getItemStack(itemID);
    }

    public static String getItemID(Item item) {
        return getPlatform().getItemID(item);
    }

    public static ArrayList<ItemStack> getAllItems() {
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
