package net.kroia.modutilities.testing.tests;

import net.kroia.modutilities.PermissionCompat;
import net.kroia.modutilities.permission.LuckPermsBridge;
import net.kroia.modutilities.testing.LuckPermsPhase;
import net.kroia.testkit.TestCategory;
import net.kroia.testkit.TestResult;
import net.kroia.testkit.TestSuite;
import net.kroia.modutilities.testing.categories.ModUtilitiesTestCategories;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Phase 2 of the "LuckPerms removed from a save" scenario (see {@link LuckPermsPhase}): the world ran with LuckPerms
 * before ({@link LuckPermsBridgeTests} recorded the phase file), now LuckPerms is gone. Active only then; every test
 * skips otherwise. Reaching these tests at all means the server started on that world without crashing.
 */
public class LuckPermsRemovedTests extends TestSuite {
    /** Registered now; the node recorded in phase 1 is not registered in this JVM (both must fall back). */
    private static final String REGISTERED_NODE = LuckPermsBridgeTests.NODE_PREFIX + "removed_check";

    public LuckPermsRemovedTests() {
        PermissionCompat.registerNode(REGISTERED_NODE, 2);
    }

    @Override
    public TestCategory getCategory() {
        return ModUtilitiesTestCategories.LUCKPERMS;
    }

    @Override
    public void registerTests() {
        addTest("lprm_bridge_not_available", phase2(() -> assertFalse("LuckPermsBridge.isAvailable()", LuckPermsBridge.isAvailable())));
        addTest("lprm_setNode_is_noop", phase2(this::testSetNodeNoop));
        addTest("lprm_getEffective_is_empty", phase2(this::testGetEffectiveEmpty));
        addTest("lprm_addNodeChangeListener_is_noop", phase2(this::testListenerNoop));
        addTest("lprm_node_check_falls_back_to_op_level", phase2(this::testFallback));
        addTest("lprm_phase1_state_survived", phase2(this::testStateSurvived));
    }

    private Supplier<TestResult> phase2(Supplier<TestResult> test) {
        return () -> {
            MinecraftServer s = getServer();
            if (s == null)
                return skip("no server");
            if (!LuckPermsPhase.isPhase2(s))
                return skip(LuckPermsPhase.phase1Exists(s) ? "LuckPerms is installed" : "world has no LuckPerms phase-1 file");
            return test.get();
        };
    }

    private TestResult testSetNodeNoop() {
        CompletableFuture<Boolean> f = LuckPermsBridge.setNode(UUID.randomUUID(), REGISTERED_NODE, true);
        return assertTrue("setNode completes at once with false", f.isDone() && !f.join());
    }

    private TestResult testGetEffectiveEmpty() {
        CompletableFuture<Optional<Boolean>> f = LuckPermsBridge.getEffective(UUID.randomUUID(), REGISTERED_NODE);
        return assertTrue("getEffective completes at once with empty", f.isDone() && f.join().isEmpty());
    }

    private TestResult testListenerNoop() {
        boolean[] called = {false};
        LuckPermsBridge.addNodeChangeListener(REGISTERED_NODE, (u, v) -> called[0] = true);
        LuckPermsBridge.setNode(UUID.randomUUID(), REGISTERED_NODE, true);
        pump(500);
        return assertFalse("listener never called", called[0]);
    }

    /** Without a permission mod a node check must equal the vanilla level check, for every level. */
    private TestResult testFallback() {
        ServerPlayer p = player();
        if (p == null)
            return skip("no online player");
        String recorded = LuckPermsPhase.expected(getServer(), "modutilities", "node");
        for (String node : recorded == null ? List.of(REGISTERED_NODE) : List.of(REGISTERED_NODE, recorded)) {
            for (int level = 0; level <= 5; level++) {
                boolean expected = PermissionCompat.hasPermission(p, level);
                boolean actual = PermissionCompat.hasPermission(p, node, level);
                if (expected != actual)
                    return TestResult.fail("", "hasPermission(player, " + node + ", " + level + ") equals the level check",
                            String.valueOf(expected), String.valueOf(actual));
                actual = PermissionCompat.hasPermission(p.createCommandSourceStack(), node, level);
                if (expected != actual)
                    return TestResult.fail("", "hasPermission(source, " + node + ", " + level + ") equals the level check",
                            String.valueOf(expected), String.valueOf(actual));
            }
        }
        return pass("node checks equal the op level checks");
    }

    private TestResult testStateSurvived() {
        MinecraftServer s = getServer();
        Map<String, String> exp = LuckPermsPhase.expectations(s, "modutilities");
        TestResult r = assertEquals("phase-1 marker", "phase1", exp.get("marker"));
        if (!r.passed())
            return r;
        ServerPlayer p = player();
        if (p == null || exp.get("player") == null || !p.getUUID().toString().equals(exp.get("player")))
            return pass("marker intact (phase-1 player not online, op level not compared)");
        return assertEquals("op level of the phase-1 player", exp.get("opLevel"),
                //String.valueOf(s.getProfilePermissions(p.getGameProfile()))); // mc<=1.21.8
                String.valueOf(s.getProfilePermissions(p.nameAndId()))); // mc>=1.21.9
    }

    private @Nullable ServerPlayer player() {
        List<ServerPlayer> players = getServer().getPlayerList().getPlayers();
        return players.isEmpty() ? null : players.get(0);
    }
}
