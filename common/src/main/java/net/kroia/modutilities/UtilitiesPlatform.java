package net.kroia.modutilities;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    public static boolean codeCalledFromServerSide()
    {
        if(Platform.getEnvironment() == Env.SERVER)
            return true;

        try {
            MinecraftServer server = getServer();
            if (server != null && server.isRunning()) {
                return server.isSameThread();
            }
        }catch(Exception ignored) {

        }
        /*
        if(Platform.getEnvironment() == Env.CLIENT)
        {
            Minecraft mc = Minecraft.getInstance();
            if(mc.level != null && !mc.level.isClientSide()) {
                return true; // Called from server side in a client environment
            }
        }*/
        return false; // Called from client side or unknown environment
    }
    /**
     * @deprecated Misspelled — use {@link #codeCalledFromClientSide()} instead.
     */
    @Deprecated
    public static boolean codeCalledFromCliendSide()
    {
        return codeCalledFromClientSide();
    }

    public static boolean codeCalledFromClientSide()
    {
        if(Platform.getEnvironment() == Env.SERVER)
            return false;

        try {
            MinecraftServer server = getServer();
            if (server != null && server.isRunning()) {
                return !server.isSameThread();
            }
        }catch(Exception ignored) {

        }
        return true;

/*
        try {
            Minecraft mc = Minecraft.getInstance();
            return mc.isSameThread();
        }catch(Exception ignored) {

        }
        return false; // Called from server side or unknown environment*/
    }
    public static boolean isClient()
    {
        return Platform.getEnvironment() == Env.CLIENT;
    }
    public static boolean isServer()
    {
        return Platform.getEnvironment() == Env.SERVER;
    }

    /*public static RegistryAccess getRegistryAccess() {
        if(codeCalledFromCliendSide())
            return getRegistryAccessClientSide();

        if(codeCalledFromServerSide())
            return getRegistryAccessServerSide();
        return null;
    }*/
    public static RegistryAccess getRegistryAccessClientSide() {
        Minecraft mc = Minecraft.getInstance();
        if(mc.level != null) {
            return mc.level.registryAccess();
        }
        return null;
    }
    public static RegistryAccess getRegistryAccessServerSide() {
        MinecraftServer server = getServer();
        if(server != null) {
            return server.registryAccess();
        }
        return null;
    }
    public static RegistryAccess getRegistryAccess() {
        if(isClient()) {
            RegistryAccess reg = getRegistryAccessClientSide();
            if(reg != null) {
                return reg;
            }
        }
        return getRegistryAccessServerSide();
    }

    public static boolean isPlatformSet() {
        return platform != null;
    }

    public static RegistryFriendlyByteBuf createRegistryFriendlyByteBufClientSide()
    {
        RegistryAccess access = getRegistryAccessClientSide();
        if(access == null)
            return null;
        return new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), access);
    }
    public static RegistryFriendlyByteBuf createRegistryFriendlyByteBufServerSide()
    {
        RegistryAccess access = getRegistryAccessServerSide();
        if(access == null)
            return null;
        return new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), access);
    }

    public static void setPlatform(PlatformAbstraction platform) {
        UtilitiesPlatform.platform = platform;
        ModUtilitiesMod.LOGGER.info("UtilitiesPlatform set to: " + platform.getPlatformType().name());
    }


    public static ItemStack getItemStack(String itemID) {
        return getPlatform().getItemStack(itemID);
    }

    public static String getItemIDStr(Item item) {
        return getPlatform().getItemIDStr(item);
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
