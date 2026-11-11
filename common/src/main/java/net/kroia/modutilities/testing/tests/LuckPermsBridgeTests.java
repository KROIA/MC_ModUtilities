package net.kroia.modutilities.testing.tests;

import net.kroia.modutilities.PermissionCompat;
import net.kroia.modutilities.permission.LuckPermsBridge;
import net.kroia.modutilities.testing.LuckPermsPhase;
import net.kroia.modutilities.testing.TestCategory;
import net.kroia.modutilities.testing.TestResult;
import net.kroia.modutilities.testing.TestSuite;
import net.kroia.modutilities.testing.categories.ModUtilitiesTestCategories;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * {@link LuckPermsBridge} and {@link PermissionCompat} node checks against a real LuckPerms (dev runs with
 * {@code -Pwith_luckperms=true}). Every test skips when LuckPerms is not available. Nodes and groups use random
 * names ({@code modutilities.test.<random>}, {@code mu_test_<random>}) and are removed again, so real data is never
 * touched. With LuckPerms it also records the phase-1 state for {@link LuckPermsRemovedTests}.
 */
public class LuckPermsBridgeTests extends TestSuite {
    static final String NODE_PREFIX = "modutilities.test.";
    private static final String GROUP_PREFIX = "mu_test_";
    private static final long TIMEOUT = 10_000;

    // Random per JVM; registered at construction (mod init) because NeoForge only collects nodes at server start.
    private final String node = NODE_PREFIX + rnd();
    private final String loopNode = NODE_PREFIX + rnd();
    private final List<Event> events = new CopyOnWriteArrayList<>();
    private final AtomicInteger loopCalls = new AtomicInteger();
    private final List<Boolean> loopValues = new CopyOnWriteArrayList<>();
    private final List<String> groups = new ArrayList<>();
    private final List<UUID> offlineUsers = new ArrayList<>();
    private final UUID fallbackSubject = UUID.randomUUID();

    private record Event(UUID player, boolean value, boolean serverThread) {}

    public LuckPermsBridgeTests() {
        PermissionCompat.registerNode(node, 4);
        // Listeners can only be added, never removed: register once, for nodes no one else uses.
        LuckPermsBridge.addNodeChangeListener(node, (uuid, value) -> events.add(new Event(uuid, value, onServerThread())));
        // Documented mirror pattern: the listener writes the value back through setNode. Must not loop.
        LuckPermsBridge.addNodeChangeListener(loopNode, (uuid, value) -> {
            loopCalls.incrementAndGet();
            loopValues.add(value);
            LuckPermsBridge.setNode(uuid, loopNode, value);
        });
    }

    @Override
    public TestCategory getCategory() {
        return ModUtilitiesTestCategories.LUCKPERMS;
    }

    @Override
    public void registerTests() {
        addTest("lp_setNode_true_effective_true", lp(this::testSetNodeTrue));
        addTest("lp_node_check_decided_by_luckperms", lp(this::testNodeCheckDecidedByLuckPerms));
        addTest("lp_setNode_false_removes_user_node", lp(this::testSetNodeFalseRemoves));
        addTest("lp_group_grant_survives_setNode_false", lp(this::testGroupGrant));
        addTest("lp_listener_fires_once_on_server_thread", lp(this::testListenerFiresOnce));
        addTest("lp_listener_write_back_does_not_loop", lp(this::testListenerNoLoop));
        addTest("lp_offline_uuid", lp(this::testOfflineUuid));
        addTest("lp_offline_read_after_write_repeated", lp(this::testOfflineReadAfterWrite));
        addTest("lp_record_phase1_state", lp(this::testRecordPhase1));
    }

    @Override
    public void teardown() {
        if (!LuckPermsBridge.isAvailable())
            return;
        cleanup(subject());
        if (player() == null)
            await(Lp.deleteUser(fallbackSubject), TIMEOUT);
        for (String g : groups)
            await(Lp.deleteGroup(g), TIMEOUT);
        for (UUID u : offlineUsers)
            await(Lp.deleteUser(u), TIMEOUT);
        groups.clear();
        offlineUsers.clear();
    }

    private Supplier<TestResult> lp(Supplier<TestResult> test) {
        return () -> {
            if (!LuckPermsBridge.isAvailable())
                return skip("LuckPerms not installed");
            if (getServer() == null)
                return skip("no server");
            return test.get();
        };
    }

    // ------------------------------------------------------------------ tests

    private TestResult testSetNodeTrue() {
        UUID u = subject();
        try {
            if (!await(LuckPermsBridge.setNode(u, node, true), TIMEOUT))
                return fail("setNode(true) returned false");
            TestResult r = assertEquals("getEffective after setNode(true)", Optional.of(true),
                    await(LuckPermsBridge.getEffective(u, node), TIMEOUT));
            if (!r.passed() || player() == null)
                return r;
            return assertTrue("PermissionCompat.hasPermission(player, node, 4)",
                    PermissionCompat.hasPermission(player(), node, 4));
        } finally {
            cleanup(u);
        }
    }

    /** The test player is op 4, so a level fallback would say true: false proves LuckPerms answered. */
    private TestResult testNodeCheckDecidedByLuckPerms() {
        ServerPlayer p = player();
        if (p == null)
            return skip("no online player");
        try {
            await(Lp.setRaw(p.getUUID(), node, false), TIMEOUT);
            TestResult r = assertFalse("explicit node=false denies an op through PermissionCompat",
                    PermissionCompat.hasPermission(p, node, 0));
            if (!r.passed())
                return r;
            await(Lp.setRaw(p.getUUID(), node, true), TIMEOUT);
            return assertTrue("explicit node=true grants", PermissionCompat.hasPermission(p, node, 4));
        } finally {
            cleanup(p.getUUID());
        }
    }

    private TestResult testSetNodeFalseRemoves() {
        UUID u = subject();
        try {
            await(LuckPermsBridge.setNode(u, node, true), TIMEOUT);
            if (!await(LuckPermsBridge.setNode(u, node, false), TIMEOUT))
                return fail("setNode(false) returned false");
            TestResult r = assertEquals("getEffective after setNode(false)", Optional.of(false),
                    await(LuckPermsBridge.getEffective(u, node), TIMEOUT));
            if (!r.passed())
                return r;
            return assertFalse("user node entry removed (not set to false)", await(Lp.hasUserNode(u, node), TIMEOUT));
        } finally {
            cleanup(u);
        }
    }

    private TestResult testGroupGrant() {
        UUID u = subject();
        String group = GROUP_PREFIX + rnd();
        groups.add(group);
        try {
            await(Lp.createGroupWith(group, node), TIMEOUT);
            await(Lp.addToGroup(u, group), TIMEOUT);
            await(LuckPermsBridge.setNode(u, node, false), TIMEOUT);
            TestResult r = assertEquals("group grant still effective after setNode(false)", Optional.of(true),
                    await(LuckPermsBridge.getEffective(u, node), TIMEOUT));
            if (!r.passed())
                return r;
        } finally {
            cleanup(u);
            await(Lp.deleteGroup(group), TIMEOUT);
            groups.remove(group);
        }
        return assertEquals("effective false after group and membership are removed", Optional.of(false),
                await(LuckPermsBridge.getEffective(u, node), TIMEOUT));
    }

    private TestResult testListenerFiresOnce() {
        UUID u = subject();
        try {
            await(LuckPermsBridge.setNode(u, node, false), TIMEOUT);
            pump(1000); // let events of earlier tests settle
            events.clear();
            await(LuckPermsBridge.setNode(u, node, true), TIMEOUT);
            pumpUntil(() -> !eventsFor(u).isEmpty(), 5000);
            pump(1500); // a duplicate would arrive in this window
            List<Event> got = eventsFor(u);
            if (got.size() != 1)
                return TestResult.fail("", "listener calls for one change", "1", got.size() + " " + got);
            Event e = got.get(0);
            TestResult r = assertTrue("listener value is true", e.value());
            return r.passed() ? assertTrue("listener ran on the server thread", e.serverThread()) : r;
        } finally {
            cleanup(u);
        }
    }

    private TestResult testListenerNoLoop() {
        UUID u = subject();
        try {
            await(LuckPermsBridge.setNode(u, loopNode, false), TIMEOUT);
            pump(1000);
            loopCalls.set(0);
            loopValues.clear();
            await(LuckPermsBridge.setNode(u, loopNode, true), TIMEOUT);
            pumpUntil(() -> loopCalls.get() >= 1, 5000);
            pump(3000); // the listener's write-back must not trigger it again
            return assertEquals("listener calls for one change with write-back (values seen: " + loopValues + ")",
                    1, loopCalls.get());
        } finally {
            cleanup(u);
        }
    }

    private TestResult testOfflineUuid() {
        UUID off = UUID.randomUUID();
        offlineUsers.add(off);
        try {
            if (getServer().getPlayerList().getPlayer(off) != null)
                return fail("random UUID is online?");
            if (!await(LuckPermsBridge.setNode(off, node, true), TIMEOUT))
                return fail("setNode(true) for an offline UUID returned false");
            TestResult r = assertEquals("offline getEffective after setNode(true)", Optional.of(true),
                    await(LuckPermsBridge.getEffective(off, node), TIMEOUT));
            if (!r.passed())
                return r;
            await(LuckPermsBridge.setNode(off, node, false), TIMEOUT);
            return assertEquals("offline getEffective after setNode(false)", Optional.of(false),
                    await(LuckPermsBridge.getEffective(off, node), TIMEOUT));
        } finally {
            await(Lp.deleteUser(off), TIMEOUT);
            offlineUsers.remove(off);
        }
    }

    /**
     * The deop pattern on an offline player, without polling: a read issued right after a write must see it, both
     * unchained (getEffective called before setNode completed) and chained on the setNode future.
     */
    private TestResult testOfflineReadAfterWrite() {
        UUID off = UUID.randomUUID();
        offlineUsers.add(off);
        try {
            for (int i = 1; i <= 5; i++) {
                LuckPermsBridge.setNode(off, node, true);
                Optional<Boolean> v = await(LuckPermsBridge.getEffective(off, node), TIMEOUT);
                if (!v.equals(Optional.of(true)))
                    return TestResult.fail("", "round " + i + ": getEffective right after setNode(true)", "Optional[true]", String.valueOf(v));
                v = await(LuckPermsBridge.setNode(off, node, false)
                        .thenCompose(ok -> LuckPermsBridge.getEffective(off, node)), TIMEOUT);
                if (!v.equals(Optional.of(false)))
                    return TestResult.fail("", "round " + i + ": setNode(false).thenCompose(getEffective)", "Optional[false]", String.valueOf(v));
            }
            return pass("5 rounds of offline write + immediate read");
        } finally {
            await(Lp.deleteUser(off), TIMEOUT);
            offlineUsers.remove(off);
        }
    }

    /** Phase 1 of the "LuckPerms removed" scenario, checked by {@link LuckPermsRemovedTests}. */
    private TestResult testRecordPhase1() {
        MinecraftServer server = getServer();
        LuckPermsPhase.record(server, "modutilities", "marker", "phase1");
        LuckPermsPhase.record(server, "modutilities", "node", node);
        ServerPlayer p = player();
        if (p != null) {
            LuckPermsPhase.record(server, "modutilities", "player", p.getUUID().toString());
            LuckPermsPhase.record(server, "modutilities", "opLevel",
                    String.valueOf(server.getProfilePermissions(p.getGameProfile()))); // mc<=1.21.8
                    //String.valueOf(server.getProfilePermissions(p.nameAndId()))); // mc>=1.21.9
        }
        return assertEquals("phase file readable", "phase1", LuckPermsPhase.expected(server, "modutilities", "marker"));
    }

    // ------------------------------------------------------------------ helpers

    private @Nullable ServerPlayer player() {
        MinecraftServer s = getServer();
        List<ServerPlayer> players = s == null ? List.of() : s.getPlayerList().getPlayers();
        return players.isEmpty() ? null : players.get(0);
    }

    /** The online test player, or a random (offline) UUID when nobody is online. */
    private UUID subject() {
        ServerPlayer p = player();
        return p != null ? p.getUUID() : fallbackSubject;
    }

    private boolean onServerThread() {
        MinecraftServer s = getServer();
        return s != null && s.isSameThread();
    }

    private List<Event> eventsFor(UUID u) {
        return events.stream().filter(e -> e.player().equals(u)).toList();
    }

    /** Removes every test node and test group membership from the user. */
    private void cleanup(UUID u) {
        await(Lp.clearTestNodes(u), TIMEOUT);
    }

    private static String rnd() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    /** Direct LuckPerms API calls the bridge does not offer. Only loaded when LuckPerms is available. */
    private static final class Lp {
        static LuckPerms api() {
            return LuckPermsProvider.get();
        }

        static CompletableFuture<Void> setRaw(UUID u, String node, boolean value) {
            return api().getUserManager().modifyUser(u, user -> {
                user.data().clear(n -> n.getKey().equalsIgnoreCase(node));
                user.data().add(Node.builder(node).value(value).build());
            });
        }

        static CompletableFuture<Boolean> hasUserNode(UUID u, String node) {
            return api().getUserManager().loadUser(u)
                    .thenApply(user -> user.getNodes().stream().anyMatch(n -> n.getKey().equalsIgnoreCase(node)));
        }

        static CompletableFuture<Void> clearTestNodes(UUID u) {
            return api().getUserManager().modifyUser(u, user -> user.data().clear(n ->
                    n.getKey().startsWith(NODE_PREFIX) || n.getKey().startsWith("group." + GROUP_PREFIX)));
        }

        static CompletableFuture<Void> createGroupWith(String group, String node) {
            GroupManager gm = api().getGroupManager();
            return gm.createAndLoadGroup(group).thenCompose(g -> {
                g.data().add(Node.builder(node).build());
                return gm.saveGroup(g);
            });
        }

        static CompletableFuture<Void> addToGroup(UUID u, String group) {
            return api().getUserManager().modifyUser(u, user -> user.data().add(InheritanceNode.builder(group).build()));
        }

        static CompletableFuture<Void> deleteGroup(String group) {
            GroupManager gm = api().getGroupManager();
            return gm.loadGroup(group).thenCompose(g -> g.map(gm::deleteGroup)
                    .orElseGet(() -> CompletableFuture.completedFuture(null)));
        }

        static CompletableFuture<Void> deleteUser(UUID u) {
            return api().getUserManager().deletePlayerData(u);
        }
    }
}
