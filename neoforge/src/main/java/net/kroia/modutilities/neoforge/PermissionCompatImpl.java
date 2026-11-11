package net.kroia.modutilities.neoforge;

import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.PermissionCompat;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.handler.DefaultPermissionHandler;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

import java.util.HashMap;
import java.util.Map;

/**
 * Node checks through NeoForge's PermissionAPI. Nodes from {@link PermissionCompat#registerNode} are registered as
 * {@code PermissionNode<Boolean>} whose default is the vanilla level check. Falls back to the level check when no
 * permission mod is the active handler, the node was not registered, or the source is not a player.
 */
public class PermissionCompatImpl {
    // Replaced on every server start (PermissionGatherEvent.Nodes fires once per start).
    private static volatile Map<String, PermissionNode<Boolean>> nodes = Map.of();

    public static void onGatherNodes(PermissionGatherEvent.Nodes event) {
        Map<String, PermissionNode<Boolean>> built = new HashMap<>();
        PermissionCompat.getRegisteredNodes().forEach((name, level) -> {
            int dot = name.indexOf('.');
            if (dot <= 0 || dot == name.length() - 1) {
                ModUtilitiesMod.LOGGER.warn("[PermissionCompat] Node '{}' must look like 'modid.name'; using level {} check only", name, level);
                return;
            }
            PermissionNode<Boolean> node = new PermissionNode<>(name.substring(0, dot), name.substring(dot + 1), PermissionTypes.BOOLEAN,
                    (player, uuid, ctx) -> player != null && PermissionCompat.hasPermission(player, level));
            event.addNodes(node);
            built.put(name, node);
        });
        nodes = built;
    }

    public static boolean hasPermission(CommandSourceStack source, String node, int fallbackLevel) {
        PermissionNode<Boolean> permNode = activeNode(node);
        if (permNode == null || !(source.getEntity() instanceof ServerPlayer player))
            return PermissionCompat.hasPermission(source, fallbackLevel);
        return PermissionAPI.getPermission(player, permNode);
    }

    public static boolean hasPermission(ServerPlayer player, String node, int fallbackLevel) {
        PermissionNode<Boolean> permNode = activeNode(node);
        if (permNode == null)
            return PermissionCompat.hasPermission(player, fallbackLevel);
        return PermissionAPI.getPermission(player, permNode);
    }

    /** @return the registered node, or null if the level check should be used instead */
    private static PermissionNode<Boolean> activeNode(String node) {
        ResourceLocation handler = PermissionAPI.getActivePermissionHandler();
        if (handler == null || handler.equals(DefaultPermissionHandler.IDENTIFIER))
            return null; // no permission mod: identical to the plain level check
        return nodes.get(node);
    }
}
