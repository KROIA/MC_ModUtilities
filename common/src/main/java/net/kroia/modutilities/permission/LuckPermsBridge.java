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
import java.util.function.Supplier;

/**
 * Optional write/notify bridge to LuckPerms, for mirroring a mod's own persisted per-player flag (e.g. an admin flag)
 * into a LuckPerms node and back. {@link net.kroia.modutilities.PermissionCompat} only <i>reads</i> nodes through the
 * platform permission APIs; those cannot write, so this talks to the LuckPerms API directly.
 * Everything is a no-op (or {@code false}) without LuckPerms. All LuckPerms references live in {@link Lp}, which is
 * only class-loaded after {@link #isAvailable()} or the presence check succeeded, so this class loads without it.
 *
 * <h2>Two-way flag sync</h2>
 * <pre>{@code
 * // mod init or server start:
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
 *   <li>{@code setNode(uuid, node, true)} sets {@code node=true} (global context) on the user, replacing a global
 *       {@code node=false} entry; if the user already has a global {@code node=true} it changes nothing, so a
 *       listener writing the value back never makes the effective value dip to unset (no event, no loop);
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

    /** @return true if LuckPerms is installed and its API is up (it comes up while the server starts) */
    public static boolean isAvailable() {
        return PRESENT && Lp.ready();
    }

    /**
     * Persists {@code node} for the player in LuckPerms, online or offline, and pushes the change to other servers
     * through LuckPerms' messaging service (if configured). {@code value=false} removes the user's global node entry
     * (see class docs). Writes and {@link #getEffective} lookups for the same player run one after another, in call
     * order.
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
     * evaluated exactly like {@link #addNodeChangeListener}. Works for offline players: they are always loaded fresh
     * from storage and handed back to LuckPerms' housekeeper ({@code cleanupUser}) afterwards. Runs after this
     * player's pending {@link #setNode} writes, so it sees them even when not chained on the {@code setNode} future.
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
     * effective value of {@code node}. Callable any time, including from server-start handlers; it stays registered
     * for every later server start. No-op without LuckPerms.
     */
    public static void addNodeChangeListener(String node, BiConsumer<UUID, Boolean> listener) {
        if (!PRESENT)
            return;
        LISTENERS.computeIfAbsent(node, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * Called once from ModUtilities' common init. Hooks the lifecycle eagerly: registering it lazily from
     * {@link #addNodeChangeListener} inside a SERVER_STARTED handler threw a ConcurrentModificationException.
     */
    public static void init() {
        if (!PRESENT)
            return;
        // LuckPerms creates a fresh API (and event bus) on every server start, so subscribe on each start.
        // check() reads LISTENERS per event, so listeners added later need no extra subscribe.
        LifecycleEvent.SERVER_STARTED.register(server -> Lp.subscribe());
        LifecycleEvent.SERVER_STOPPED.register(server -> Lp.LAST_SEEN.clear());
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

        // player -> tail of that player's queued reads and writes
        static final Map<UUID, CompletableFuture<?>> QUEUE = new ConcurrentHashMap<>();

        /**
         * Runs {@code job} after every job queued before it for this player. modifyUser = load from storage + edit +
         * save, and an offline read's loadUser reloads the same live User from storage. Any overlap for one player
         * loses data: a second load resets the live user before the first save landed (effective value dips, a
         * write-back listener loops), or wipes the in-flight edit so modifyUser saves nothing. So reads and writes
         * per player run strictly one after another.
         */
        private static <T> CompletableFuture<T> enqueue(UUID player, Supplier<CompletableFuture<T>> job) {
            CompletableFuture<T> mine = new CompletableFuture<>();
            CompletableFuture<?> prev = QUEUE.put(player, mine);
            (prev == null ? CompletableFuture.completedFuture(null) : prev)
                    .handle((x, t) -> null) // a failed predecessor must not block the queue
                    .thenCompose(ignored -> job.get())
                    .whenComplete((v, t) -> {
                        QUEUE.remove(player, mine);
                        if (t != null) mine.completeExceptionally(t);
                        else mine.complete(v);
                    });
            return mine;
        }

        static CompletableFuture<Boolean> setNode(UUID player, String node, boolean value) {
            return enqueue(player, () -> write(player, node, value)).handle((ok, t) -> ok != null && ok);
        }

        private static CompletableFuture<Boolean> write(UUID player, String node, boolean value) {
            LuckPerms api = api();
            User[] modified = new User[1];
            return api.getUserManager().modifyUser(player, user -> {
                modified[0] = user;
                if (!value) {
                    user.data().clear(n -> isGlobal(n, node));
                    return;
                }
                // Never remove an existing true entry: readers on other threads would briefly see the node unset,
                // fire listeners with false, and a listener writing that back would start a loop.
                if (user.getNodes().stream().anyMatch(n -> isGlobal(n, node) && n.getValue()))
                    return;
                user.data().clear(n -> isGlobal(n, node) && !n.getValue());
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

        private static boolean isGlobal(Node n, String node) {
            return n.getKey().equalsIgnoreCase(node) && n.getContexts().isEmpty();
        }

        static CompletableFuture<Optional<Boolean>> getEffective(UUID player, String node) {
            // queued behind this player's earlier writes (so it sees them) and ahead of later ones (see enqueue)
            return enqueue(player, () -> read(player, node))
                    .exceptionally(t -> {
                        ModUtilitiesMod.LOGGER.error("[LuckPerms] Failed to look up {} for {}", node, player, t);
                        return Optional.empty();
                    });
        }

        private static CompletableFuture<Optional<Boolean>> read(UUID player, String node) {
            UserManager users = api().getUserManager();
            MinecraftServer server = UtilitiesPlatform.getServer();
            User online = server != null && server.getPlayerList().getPlayer(player) != null ? users.getUser(player) : null;
            if (online != null) // LuckPerms keeps online users current
                return CompletableFuture.completedFuture(Optional.of(effective(online.getCachedData(), node).asBoolean()));
            // Offline: a still-loaded instance (e.g. left over from modifyUser) can hold stale cached data, so always
            // load from storage, read, then let LuckPerms unload it (no-op if the player is online by now).
            return users.loadUser(player).thenApply(user -> {
                try {
                    user.getCachedData().invalidate();
                    return Optional.of(effective(user.getCachedData(), node).asBoolean());
                } finally {
                    users.cleanupUser(user);
                }
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
