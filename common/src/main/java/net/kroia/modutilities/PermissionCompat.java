package net.kroia.modutilities;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;

/**
 * Version-agnostic vanilla permission-level checks (0 = all, 2 = gamemasters/OP, 4 = owners), so
 * dependent mods need no Minecraft-version toggles of their own. Minecraft 1.21.11 replaced
 * {@code CommandSourceStack.hasPermission(int)} and {@code Player.hasPermissions(int)} with permission sets.
 */
public final class PermissionCompat {
    private PermissionCompat() {}

    /** @return true if the command source has at least the given vanilla permission level */
    public static boolean hasPermission(CommandSourceStack source, int level) {
        //return source.hasPermission(level); // mc<=1.21.10
        return source.permissions().hasPermission(levelPermission(level)); // mc>=1.21.11
    }

    /** @return true if the player has at least the given vanilla permission level */
    public static boolean hasPermission(Player player, int level) {
        //return player.hasPermissions(level); // mc<=1.21.10
        return player.permissions().hasPermission(levelPermission(level)); // mc>=1.21.11
    }

    private static net.minecraft.server.permissions.Permission levelPermission(int level) { // mc>=1.21.11
        return new net.minecraft.server.permissions.Permission.HasCommandLevel(net.minecraft.server.permissions.PermissionLevel.byId(level)); // mc>=1.21.11
    } // mc>=1.21.11
}
