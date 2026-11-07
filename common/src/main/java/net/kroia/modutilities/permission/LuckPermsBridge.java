package net.kroia.modutilities.permission;

import dev.architectury.event.events.common.LifecycleEvent;
import net.kroia.modutilities.ModChecker;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.UtilitiesPlatform;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedDataManager;
import net.luckperms.api.event.group.GroupDataRecalculateEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.util.Tristate;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * Optional write/notify bridge to LuckPerms, for mirroring a mod's own persisted per-player flag (e.g. an admin flag)
 * into a LuckPerms node and back. {@link net.kroia.modutilities.PermissionCompat} only <i>reads</i> nodes through the
 * platform permission APIs; those cannot write, so this talks to the LuckPerms API directly.
 * Everything is a no-op (or {@code false}) without LuckPerms. All LuckPerms references live in {@link Lp}, which is
 * only class-loaded after {@link #isAvailable()} or the presence check succeeded, so this class loads without it.
 *
 * <h2>Two-way flag sync</h2>
 * <pre>{@code
 * // mod init:
 * LuckPermsBridge.addNodeChangeListener("banksystem.admin", (uuid, admin) -> {
 *     // Runs on the server thread. Write the flag DIRECTLY, never through the path that calls setNode.
 *     // Our own setNode also ends up here; the flag already has that value, so this is a no-op (loop-safe).
 *     if (data.isAdmin(uuid) != admin)
 *         data.setAdminFlagOnly(uuid, admin);
 * });
 *
 * // "/banksystem op|deop <player>":
 * data.setAdminFlagOnly(uuid, admin);
 * LuckPermsBridge.setNode(uuid, "banksystem.admin", admin); // persists in LuckPerms (offline players too)
 * }</pre>
 *
 * <h2>Semantics</h2>
 * <ul>
 *   <li>{@code setNode(uuid, node, true)} sets {@code node=true} (global context) on the user;
 *       {@code setNode(uuid, node, false)} <b>removes</b> the user's global {@code node} entry (true or false) instead
 *       of adding {@code node=false}, so group grants keep working. Consequence: deop cannot override a grant the
 *       player inherits from a group; the listener then reports {@code true} again and the flag follows LuckPerms.</li>
 *   <li>The listener receives the <b>effective</b> value (user node, groups, wildcards; unset = {@code false}). It
 *       fires when that value changes: for local edits (commands, web editor, API), for edits made on another server
 *       once LuckPerms' messaging service syncs the user here, and for group edits (re-checked for every user
 *       LuckPerms has loaded, i.e. online players).</li>
 *   <li>The first observation of a user in a server session only fires if the node is defined (true or false), so an
 *       unset node never wipes flags that predate LuckPerms. Changes made while a player was neither online nor
 *       edited here are picked up at their next login only if they define the node.</li>
 * </ul>
 */
public final class LuckPermsBridge {
    private LuckPermsBridge() {}

    private static final boolean PRESENT = ModChecker.isModLoaded("luckperms") && classPresent();
    private static final Map<String, List<BiConsumer<UUID, Boolean>>> LISTENERS = new ConcurrentHashMap<>();
    private static boolean lifecycleHooked;

    /** @return true if LuckPerms is installed and its API is up (it comes up while the server starts) */
    public static boolean isAvailable() {
        return PRESENT && Lp.ready();
    }

    /**
     * Persists {@code node} for the player in LuckPerms, online or offline, and pushes the change to other servers
     * through LuckPerms' messaging service (if configured). {@code value=false} removes the user's global node entry
     * (see class docs).
     *
     * @return completes with true once saved; false if LuckPerms is absent or the update failed (logged)
     */
    public static CompletableFuture<Boolean> setNode(UUID player, String node, boolean value) {
        if (!isAvailable())
            return CompletableFuture.completedFuture(false);
        return Lp.setNode(player, node, value);
    }

    /**
     * Looks up the player's <b>effective</b> value of {@code node} (user node, groups, wildcards; unset = {@code false}),
     * evaluated exactly like {@link #addNodeChangeListener}. Works for offline players: they are loaded from storage
     * and handed back to LuckPerms' housekeeper ({@code cleanupUser}) afterwards.
     * The future completes on a LuckPerms thread; hop to the server thread before touching game state.
     *
     * <p>Use case: {@code setNode(uuid, node, false)} only removes the user node. If a group still grants it, the
     * effective value stays {@code true} and no change event fires, so check afterwards. Chain it on the
     * {@code setNode} future: once that completes the write is applied and the user's cached data invalidated, so
     * this lookup sees the post-write state.
     * <pre>{@code
     * // "/banksystem deop <player>":
     * data.setAdminFlagOnly(uuid, false);
     * LuckPermsBridge.setNode(uuid, "banksystem.admin", false)
     *         .thenCompose(ok -> LuckPermsBridge.getEffective(uuid, "banksystem.admin"))
     *         .thenAccept(v -> server.execute(() -> {
     *             if (v.equals(Optional.of(true))) {
     *                 data.setAdminFlagOnly(uuid, true);
     *                 source.sendFailure(Component.literal("Player is still admin through a LuckPerms group"));
     *             }
     *         }));
     * }</pre>
     *
     * @return the effective value; empty if LuckPerms is absent or the lookup failed (logged)
     */
    public static CompletableFuture<Optional<Boolean>> getEffective(UUID player, String node) {
        if (!isAvailable())
            return CompletableFuture.completedFuture(Optional.empty());
        return Lp.getEffective(player, node);
    }

    /**
     * Calls {@code listener(player, effectiveValue)} on the server thread whenever LuckPerms changes the player's
     * effective value of {@code node}. Call during mod init; it stays registered for every server start.
     * No-op without LuckPerms.
     */
    public static synchronized void addNodeChangeListener(String node, BiConsumer<UUID, Boolean> listener) {
        if (!PRESENT)
            return;
        LISTENERS.computeIfAbsent(node, k -> new CopyOnWriteArrayList<>()).add(listener);
        if (!lifecycleHooked) {
            lifecycleHooked = true;
            // LuckPerms creates a fresh API (and event bus) on every server start, so subscribe on each start.
            LifecycleEvent.SERVER_STARTED.register(server -> Lp.subscribe());
            LifecycleEvent.SERVER_STOPPED.register(server -> Lp.LAST_SEEN.clear());
            if (isAvailable())
                Lp.subscribe(); // registered after the server started
        }
    }

    private static boolean classPresent() {
        try {
            Class.forName("net.luckperms.api.LuckPermsProvider", false, LuckPermsBridge.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /** Only loaded when LuckPerms is present. */
    private static final class Lp {
        // server thread only: node -> player -> last effective value seen this server session
        static final Map<String, Map<UUID, Tristate>> LAST_SEEN = new HashMap<>();
        static LuckPerms subscribedTo;

        static boolean ready() {
            return api() != null;
        }

        static LuckPerms api() {
            try {
                return LuckPermsProvider.get();
            } catch (IllegalStateException e) { // not loaded yet / already shut down
                return null;
            }
        }

        static CompletableFuture<Boolean> setNode(UUID player, String node, boolean value) {
            LuckPerms api = api();
            User[] modified = new User[1];
            return api.getUserManager().modifyUser(player, user -> {
                modified[0] = user;
                user.data().clear(n -> n.getKey().equalsIgnoreCase(node) && n.getContexts().isEmpty());
                if (value)
                    user.data().add(Node.builder(node).value(true).build());
            }).thenApply(v -> {
                // the node edits already invalidate the cache; be explicit so a following getEffective sees the write
                modified[0].getCachedData().invalidate();
                // modifyUser only saves; tell other servers on the same storage (what LP's own commands do)
                api.getMessagingService().ifPresent(ms -> ms.pushUserUpdate(modified[0]));
                return true;
            }).exceptionally(t -> {
                ModUtilitiesMod.LOGGER.error("[LuckPerms] Failed to set {}={} for {}", node, value, player, t);
                return false;
            });
        }

        static CompletableFuture<Optional<Boolean>> getEffective(UUID player, String node) {
            UserManager users = api().getUserManager();
            User loaded = users.getUser(player);
            if (loaded != null)
                return CompletableFuture.completedFuture(Optional.of(effective(loaded.getCachedData(), node).asBoolean()));
            // offline: load, read, then let LuckPerms unload it (no-op if the player is online by now)
            return users.loadUser(player).thenApply(user -> {
                try {
                    return Optional.of(effective(user.getCachedData(), node).asBoolean());
                } finally {
                    users.cleanupUser(user);
                }
            }).exceptionally(t -> {
                ModUtilitiesMod.LOGGER.error("[LuckPerms] Failed to look up {} for {}", node, player, t);
                return Optional.empty();
            });
        }

        // Shared by the listener and getEffective so both agree on what "effective" means.
        private static Tristate effective(CachedDataManager data, String node) {
            return data.getPermissionData().checkPermission(node);
        }

        static synchronized void subscribe() {
            LuckPerms api = api();
            if (api == null || api == subscribedTo)
                return;
            subscribedTo = api;
            // Fired (async) after every user data invalidation: node edits, web editor, cross-server sync, login.
            api.getEventBus().subscribe(UserDataRecalculateEvent.class,
                    e -> onServerThread(() -> check(e.getUser().getUniqueId(), e.getData())));
            // Group edits invalidate member caches silently; re-check every loaded (online) user.
            api.getEventBus().subscribe(GroupDataRecalculateEvent.class,
                    e -> onServerThread(() -> api.getUserManager().getLoadedUsers()
                            .forEach(u -> check(u.getUniqueId(), u.getCachedData()))));
        }

        private static void onServerThread(Runnable r) {
            MinecraftServer server = UtilitiesPlatform.getServer();
            if (server != null)
                server.execute(r);
        }

        // Reads the live cache when it runs, so out-of-order async events still end on the latest state.
        private static void check(UUID player, CachedDataManager data) {
            LISTENERS.forEach((node, listeners) -> {
                Tristate now = effective(data, node);
                Tristate before = LAST_SEEN.computeIfAbsent(node, k -> new HashMap<>()).put(player, now);
                if (now == before || (before == null && now == Tristate.UNDEFINED))
                    return;
                for (BiConsumer<UUID, Boolean> l : listeners) {
                    try {
                        l.accept(player, now.asBoolean());
                    } catch (Exception ex) {
                        ModUtilitiesMod.LOGGER.error("[LuckPerms] Listener for {} failed", node, ex);
                    }
                }
            });
        }
    }
}
