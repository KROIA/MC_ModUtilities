package net.kroia.modutilities.fabric;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kroia.modutilities.PermissionCompat;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * Node checks through fabric-permissions-api (bundled). A permission mod such as LuckPerms answers TRUE/FALSE;
 * DEFAULT (no permission mod, or node not set) falls back to the vanilla level check.
 */
public class PermissionCompatImpl {
    public static boolean hasPermission(CommandSourceStack source, String node, int fallbackLevel) {
        if (!(source.getEntity() instanceof ServerPlayer))
            return PermissionCompat.hasPermission(source, fallbackLevel);
        return Permissions.getPermissionValue(source, node).orElseGet(() -> PermissionCompat.hasPermission(source, fallbackLevel));
    }

    public static boolean hasPermission(ServerPlayer player, String node, int fallbackLevel) {
        return Permissions.getPermissionValue(player, node).orElseGet(() -> PermissionCompat.hasPermission(player, fallbackLevel));
    }
}
