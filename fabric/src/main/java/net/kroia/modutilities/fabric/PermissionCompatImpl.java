package net.kroia.modutilities.fabric;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.kroia.modutilities.PermissionCompat;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * Node checks through fabric-permissions-api (bundled). A permission mod such as LuckPerms answers TRUE/FALSE;
 * DEFAULT (no permission mod, or node not set) falls back to the vanilla level check.
 * If the API is not loaded (e.g. dependent mods' dev runs, where the jar-in-jar is not unpacked),
 * the vanilla level check is used. All Permissions references live in {@link Api} so this class loads without it.
 */
public class PermissionCompatImpl {
    private static final boolean API_LOADED = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");

    public static boolean hasPermission(CommandSourceStack source, String node, int fallbackLevel) {
        if (!API_LOADED || !(source.getEntity() instanceof ServerPlayer))
            return PermissionCompat.hasPermission(source, fallbackLevel);
        return Api.hasPermission(source, node, fallbackLevel);
    }

    public static boolean hasPermission(ServerPlayer player, String node, int fallbackLevel) {
        if (!API_LOADED)
            return PermissionCompat.hasPermission(player, fallbackLevel);
        return Api.hasPermission(player, node, fallbackLevel);
    }

    private static final class Api {
        static boolean hasPermission(CommandSourceStack source, String node, int fallbackLevel) {
            return Permissions.getPermissionValue(source, node).orElseGet(() -> PermissionCompat.hasPermission(source, fallbackLevel));
        }

        static boolean hasPermission(ServerPlayer player, String node, int fallbackLevel) {
            return Permissions.getPermissionValue(player, node).orElseGet(() -> PermissionCompat.hasPermission(player, fallbackLevel));
        }
    }
}
