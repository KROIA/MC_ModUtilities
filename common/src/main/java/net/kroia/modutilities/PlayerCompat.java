package net.kroia.modutilities;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Version-agnostic player profile accessors, so dependent mods need no Minecraft-version toggles
 * of their own. Minecraft 1.21.9 ships authlib 7, where {@link GameProfile} became a record
 * ({@code getName()}/{@code getId()} were replaced by {@code name()}/{@code id()}).
 */
public final class PlayerCompat {
    private PlayerCompat() {}

    /** @return the player name stored in the profile */
    public static String profileName(GameProfile profile) {
        //return profile.getName(); // mc<=1.21.8
        return profile.name(); // mc>=1.21.9
    }

    /** @return the player UUID stored in the profile */
    public static UUID profileId(GameProfile profile) {
        //return profile.getId(); // mc<=1.21.8
        return profile.id(); // mc>=1.21.9
    }

    /** Shows a message to the player: in the chat, or above the hotbar if {@code actionBar}. */
    public static void sendMessage(Player player, Component message, boolean actionBar) {
        //player.displayClientMessage(message, actionBar); // mc<=1.21.11
        if (actionBar) player.sendOverlayMessage(message); else player.sendSystemMessage(message); // mc>=26.1
    }

    /** Puts the stack into the player's inventory, or drops it at the player's feet if the inventory is full. */
    public static void giveOrDrop(Player player, net.minecraft.world.item.ItemStack stack) {
        if (!player.getInventory().add(stack)) drop(player, stack);
    }

    /** Drops the stack in front of the player, like pressing the drop key (26.3 added a {@code Prediction} argument). */
    public static void drop(Player player, net.minecraft.world.item.ItemStack stack) {
        //player.drop(stack, false); // mc<=26.2
        player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY); // mc>=26.3
    }
}
