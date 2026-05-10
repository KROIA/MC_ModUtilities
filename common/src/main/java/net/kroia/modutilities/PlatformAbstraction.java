package net.kroia.modutilities;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

/**
 * Service-provider interface implemented per modloader (Fabric, NeoForge, Forge, Quilt) to
 * supply platform-specific access to registries and the running server instance.
 * <p>
 * The active implementation is registered through {@link UtilitiesPlatform#setPlatform(PlatformAbstraction)}
 * during mod initialization and accessed through the static helpers on
 * {@link UtilitiesPlatform}; consumers should normally call those helpers instead of using
 * this interface directly.
 */
public interface PlatformAbstraction {
    /**
     * Resolves an {@link ItemStack} (count 1) from the given fully qualified item ID.
     *
     * @param itemIDStr the item registry ID, e.g. {@code "minecraft:stone"}
     * @return a single-item stack, or {@code null}/{@link ItemStack#EMPTY} if unknown
     */
    ItemStack getItemStack(String itemIDStr);
    /**
     * Returns the canonical {@code namespace:path} registry ID for the given item.
     *
     * @param item the item to look up
     * @return the registry ID string
     */
    String getItemIDStr(Item item);
    /**
     * Returns one {@link ItemStack} per registered item on this platform.
     *
     * @return a list of all items as stacks
     */
    ArrayList<ItemStack> getAllItems();
    /**
     * Returns the currently running {@link MinecraftServer}, if any.
     *
     * @return the server instance, or {@code null} if no server is running
     */
    MinecraftServer getServer();
    /**
     * Identifies which modloader this implementation targets.
     *
     * @return the {@link UtilitiesPlatform.Type} for this platform
     */
    UtilitiesPlatform.Type getPlatformType();
}
