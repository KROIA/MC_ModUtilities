package net.kroia.modutilities;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Static facade for the active {@link PlatformAbstraction} implementation.
 * <p>
 * During mod initialization each platform module registers its own {@link PlatformAbstraction}
 * via {@link #setPlatform(PlatformAbstraction)}; consumers then call the helpers exposed
 * here (e.g. {@link #getItemStack(String)}, {@link #getServer()}) without needing to know
 * which modloader is hosting the code.
 * <p>
 * Also provides convenience predicates for distinguishing client- and server-side code paths
 * and for obtaining a {@link RegistryAccess} appropriate to the current side.
 */
public class UtilitiesPlatform {
    /**
     * Identifies the modloader the active platform implementation targets.
     */
    public enum Type {
        /** The Fabric modloader. */
        FABRIC,
        /** The legacy Forge modloader. */
        FORGE,
        /** The Quilt modloader. */
        QUILT,
        /** The NeoForge modloader. */
        NEOFORGE
    }
    private static PlatformAbstraction platform;

    /**
     * Returns the registered platform implementation.
     *
     * @return the active {@link PlatformAbstraction}
     * @throws IllegalStateException if no implementation has been registered yet
     * @apiNote Use {@link #isPlatformSet()} for a non-throwing check.
     */
    public static PlatformAbstraction getPlatform() {
        if (platform == null) {
            throw new IllegalStateException(ModUtilitiesMod.MOD_ID+" Platform not set!");
        }
        return platform;
    }

    /**
     * Determines whether the calling code is executing on the server side.
     * <p>
     * On dedicated servers this always returns {@code true}. On the client side or in
     * integrated-server scenarios it returns {@code true} when invoked from the server
     * thread of a running integrated server.
     *
     * @return {@code true} if the call originates from server-side code, otherwise {@code false}
     */
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

    /**
     * Determines whether the calling code is executing on the client side.
     * <p>
     * On dedicated servers this always returns {@code false}. In integrated-server
     * scenarios the call is considered client-side when not on the server thread.
     *
     * @return {@code true} if the call originates from client-side code, otherwise {@code false}
     */
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
    /**
     * Returns whether this JVM is running in a client environment (physical client).
     *
     * @return {@code true} on Minecraft clients, {@code false} on dedicated servers
     */
    public static boolean isClient()
    {
        return Platform.getEnvironment() == Env.CLIENT;
    }
    /**
     * Returns whether this JVM is running in a dedicated-server environment.
     *
     * @return {@code true} on dedicated servers, {@code false} on physical clients
     */
    public static boolean isServer()
    {
        return Platform.getEnvironment() == Env.SERVER;
    }

    /**
     * Returns the {@link RegistryAccess} associated with the client's currently loaded level.
     *
     * @return the client-side registry access, or {@code null} if no level is loaded
     * @apiNote Must only be called from a physical client.
     */
    @Environment(EnvType.CLIENT)
    public static RegistryAccess getRegistryAccessClientSide() {
        Minecraft mc = Minecraft.getInstance();
        if(mc.level != null) {
            return mc.level.registryAccess();
        }
        return null;
    }
    /**
     * Returns the {@link RegistryAccess} associated with the running {@link MinecraftServer}.
     *
     * @return the server-side registry access, or {@code null} if no server is running
     */
    public static RegistryAccess getRegistryAccessServerSide() {
        MinecraftServer server = getServer();
        if(server != null) {
            return server.registryAccess();
        }
        return null;
    }
    /**
     * Returns the most appropriate {@link RegistryAccess} for the current side.
     * <p>
     * On a physical client this first tries {@link #getRegistryAccessClientSide()} and falls
     * back to the server-side registry. The {@link #isClient()} guard ensures the
     * client-only method is not invoked on a dedicated server.
     *
     * @return the active registry access, or {@code null} if neither side is available
     */
    public static RegistryAccess getRegistryAccess() {
        if(isClient()) {
            RegistryAccess reg = getRegistryAccessClientSide();
            if(reg != null) {
                return reg;
            }
        }
        return getRegistryAccessServerSide();
    }

    /**
     * Non-throwing check for whether a {@link PlatformAbstraction} has been registered.
     *
     * @return {@code true} if a platform implementation is available, otherwise {@code false}
     * @apiNote Unlike {@link #getPlatform()} this never throws; use it in early-init code paths.
     */
    public static boolean isPlatformSet() {
        return platform != null;
    }

    /**
     * Allocates a new empty {@link RegistryFriendlyByteBuf} bound to the client-side registries.
     *
     * @return a new buffer, or {@code null} if no client-side registry access is available
     */
    public static @Nullable RegistryFriendlyByteBuf createRegistryFriendlyByteBufClientSide()
    {
        RegistryAccess access = getRegistryAccessClientSide();
        if(access == null)
            return null;
        return new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), access);
    }
    /**
     * Allocates a new empty {@link RegistryFriendlyByteBuf} bound to the server-side registries.
     *
     * @return a new buffer, or {@code null} if no server is running
     */
    public static @Nullable RegistryFriendlyByteBuf createRegistryFriendlyByteBufServerSide()
    {
        RegistryAccess access = getRegistryAccessServerSide();
        if(access == null)
            return null;
        return new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), access);
    }

    /**
     * Registers the platform-specific {@link PlatformAbstraction} implementation.
     * <p>
     * This is invoked once per JVM during mod initialization by the active platform module.
     *
     * @param platform the platform implementation to register
     */
    public static void setPlatform(PlatformAbstraction platform) {
        UtilitiesPlatform.platform = platform;
        ModUtilitiesMod.LOGGER.info("UtilitiesPlatform set to: " + platform.getPlatformType().name());
    }


    /**
     * Resolves an {@link ItemStack} from the given fully qualified item ID via the active platform.
     *
     * @param itemID the item registry ID (e.g. {@code "minecraft:stone"})
     * @return a single-item stack, or {@code null}/{@link ItemStack#EMPTY} if unknown
     */
    public static ItemStack getItemStack(String itemID) {
        return getPlatform().getItemStack(itemID);
    }

    /**
     * Returns the canonical {@code namespace:path} registry ID for the given item via the active platform.
     *
     * @param item the item to look up
     * @return the registry ID string
     */
    public static String getItemIDStr(Item item) {
        return getPlatform().getItemIDStr(item);
    }

    /**
     * Returns one {@link ItemStack} per registered item known to the active platform.
     *
     * @return a list of all items as stacks
     */
    public static ArrayList<ItemStack> getAllItems() {
        return getPlatform().getAllItems();
    }
    /**
     * Returns the running {@link MinecraftServer} via the active platform.
     *
     * @return the server instance, or {@code null} if no server is running
     */
    public static MinecraftServer getServer() {
        return getPlatform().getServer();
    }
    /**
     * Returns the modloader type of the active platform implementation.
     *
     * @return the {@link Type} of the active platform
     */
    public static Type getPlatformType() {
        return getPlatform().getPlatformType();
    }

    /**
     * Convenience predicate for {@link Type#FORGE}.
     *
     * @return {@code true} if running on Forge
     */
    public static boolean isForge() {
        return getPlatformType() == Type.FORGE;
    }
    /**
     * Convenience predicate for {@link Type#FABRIC}.
     *
     * @return {@code true} if running on Fabric
     */
    public static boolean isFabric() {
        return getPlatformType() == Type.FABRIC;
    }
    /**
     * Convenience predicate for {@link Type#QUILT}.
     *
     * @return {@code true} if running on Quilt
     */
    public static boolean isQuilt() {
        return getPlatformType() == Type.QUILT;
    }
}
