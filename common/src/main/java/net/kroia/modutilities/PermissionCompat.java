package net.kroia.modutilities;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Version-agnostic vanilla permission-level checks (0 = all, 2 = gamemasters/OP, 4 = owners), so
 * dependent mods need no Minecraft-version toggles of their own. Minecraft 1.21.11 replaced
 * {@code CommandSourceStack.hasPermission(int)} and {@code Player.hasPermissions(int)} with permission sets.
 *
 * <h2>Permission nodes</h2>
 * Node checks let server admins grant or deny rights through a permission mod such as LuckPerms.
 * Fabric/Quilt route them through the bundled fabric-permissions-api, NeoForge through its {@code PermissionAPI}.
 * Without a permission mod a node check behaves exactly like the vanilla level check with {@code fallbackLevel}.
 * <pre>{@code
 * // mod init (before the server starts; required for NeoForge):
 * PermissionCompat.registerNode("banksystem.admin.command.backup", 2);
 *
 * // command:
 * .requires(src -> PermissionCompat.hasPermission(src, "banksystem.admin.command.backup", 2))
 * }</pre>
 * Limitations: a node is only resolved for a player online on this server. Console, command blocks and other
 * non-player sources always use the level check. On NeoForge an unregistered node (or one registered after the
 * server started) also falls back to the level check.
 */
public final class PermissionCompat {
    private PermissionCompat() {}

    private static final Map<String, Integer> NODES = new ConcurrentHashMap<>();

    /**
     * Declares a permission node. Call during mod init, before the server starts; NeoForge collects nodes once per
     * server start. Re-registering a node overwrites its fallback level.
     *
     * @param node          full node name, lowercase and dot-separated, starting with the mod id
     *                      (e.g. {@code "banksystem.admin.command.backup"})
     * @param fallbackLevel vanilla permission level used when no permission mod decides; pass the same value to
     *                      {@link #hasPermission(CommandSourceStack, String, int)}
     */
    public static void registerNode(String node, int fallbackLevel) {
        NODES.put(node, fallbackLevel);
    }

    /** @return read-only view of all registered nodes and their fallback levels (used by the platform layers) */
    public static Map<String, Integer> getRegisteredNodes() {
        return Collections.unmodifiableMap(NODES);
    }

    /**
     * Node check for a command source. Player sources are checked against the permission mod; other sources
     * (console, command blocks) and setups without a permission mod use {@link #hasPermission(CommandSourceStack, int)}.
     *
     * @return true if the source has the node, or (no permission mod / not a player) at least {@code fallbackLevel}
     */
    @ExpectPlatform
    public static boolean hasPermission(CommandSourceStack source, String node, int fallbackLevel) {
        throw new AssertionError();
    }

    /**
     * Node check for an online player on this server. Without a permission mod this equals
     * {@link #hasPermission(Player, int)}.
     *
     * @return true if the player has the node, or (no permission mod) at least {@code fallbackLevel}
     */
    @ExpectPlatform
    public static boolean hasPermission(ServerPlayer player, String node, int fallbackLevel) {
        throw new AssertionError();
    }

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
