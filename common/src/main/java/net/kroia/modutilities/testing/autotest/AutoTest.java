package net.kroia.modutilities.testing.autotest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.multi_server.MultiServerConfig;
import net.kroia.modutilities.networking.multi_server.MultiServerManager;
import net.kroia.modutilities.testing.TestRegistry;
import net.kroia.modutilities.testing.TestResult;
import net.kroia.modutilities.testing.TestRunner;
import net.kroia.modutilities.testing.TestSuite;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Unattended test runs for tools/autotest/run_matrix.py. Dev builds only (see {@code DevBootstrap}).
 * Inert unless the JVM is started with {@code -Dmodutilities.autotest=<instance label>}.
 * <p>
 * Server side (dedicated or integrated): once every readiness condition holds, runs every registered
 * suite allowed on this server type, writes a JSON report and (dedicated, {@code stop=true}) stops the
 * server. Client side: see {@link AutoTestClient}.
 * <p>
 * System properties ({@code modutilities.autotest.} prefix):
 * <ul>
 *   <li>{@code results} report path (default {@code autotest-results.json}); the client writes
 *       {@code <results>.client.json} next to it</li>
 *   <li>{@code waitPlayer} wait for a player to join before running (default true)</li>
 *   <li>{@code waitSlaves} master: wait for this many connected slaves (default 0)</li>
 *   <li>{@code after} wait until this file exists (sequencing between instances)</li>
 *   <li>{@code stop} dedicated server: stop after the report is written (default true)</li>
 *   <li>{@code timeout} watchdog in seconds; on expiry a timeout report is written and the JVM halts (default 900)</li>
 *   <li>{@code mods} comma separated mod ids to run (default: all mods with registered suites)</li>
 *   <li>{@code connect} / {@code world} client only, see {@link AutoTestClient}</li>
 * </ul>
 * The multi-server role comes from the instance's {@code config/modutilities/MultiServerConfig.json}. If that
 * config is enabled but no mod created the link by the first server tick (ModUtilities running
 * alone), AutoTest creates it itself.
 */
public final class AutoTest {
    private AutoTest() {}

    static final String P = "modutilities.autotest";
    static final String LABEL = System.getProperty(P);
    static final Path RESULTS = Paths.get(System.getProperty(P + ".results", "autotest-results.json")).toAbsolutePath();
    private static final boolean WAIT_PLAYER = Boolean.parseBoolean(System.getProperty(P + ".waitPlayer", "true"));
    private static final int WAIT_SLAVES = Integer.getInteger(P + ".waitSlaves", 0);
    private static final @Nullable String AFTER = System.getProperty(P + ".after");
    private static final boolean STOP = Boolean.parseBoolean(System.getProperty(P + ".stop", "true"));
    private static final long TIMEOUT_SECONDS = Long.getLong(P + ".timeout", 900L);
    private static final @Nullable String MODS = System.getProperty(P + ".mods");
    /** Ticks every readiness condition must hold before the run starts (lets joins and syncs settle). */
    private static final int SETTLE_TICKS = 40;

    static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final long START_MILLIS = System.currentTimeMillis();

    private static volatile boolean finished;
    private static int tick;
    private static int readyTicks;
    private static String waitingFor = "server start";
    private static boolean ownsMultiServer;

    public static boolean isEnabled() {
        return LABEL != null;
    }

    /** @return true once this JVM's server wrote its report (the client polls this in singleplayer). */
    public static boolean isFinished() {
        return finished;
    }

    public static void init() {
        if (!isEnabled()) return;
        ModUtilitiesMod.LOGGER.info("[AutoTest] enabled, instance '{}', report {}", LABEL, RESULTS);

        LifecycleEvent.SERVER_STARTED.register(server -> server.setDifficulty(Difficulty.PEACEFUL, true));
        TickEvent.SERVER_POST.register(AutoTest::serverTick);
        LifecycleEvent.SERVER_STOPPING.register(server -> {
            if (ownsMultiServer) MultiServerManager.cleanup();
        });
        // The dev launcher (Architectury TransformerRuntime) leaves two non-daemon thread pools behind,
        // so a dedicated dev server's JVM never exits on its own after the server stopped.
        // Exit from a separate thread: vanilla's shutdown hook waits for the server thread to finish.
        LifecycleEvent.SERVER_STOPPED.register(server -> {
            if (server.isDedicatedServer()) new Thread(() -> System.exit(0), "ModUtilities-AutoTest-Exit").start();
        });
        if (Platform.getEnvironment() == Env.CLIENT) {
            AutoTestClient.init();
        }

        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(TIMEOUT_SECONDS * 1000L);
            } catch (InterruptedException e) {
                return;
            }
            ModUtilitiesMod.LOGGER.error("[AutoTest] watchdog: no result after {} s (waiting for: {}), halting", TIMEOUT_SECONDS, waitingFor);
            if (!finished) {
                JsonObject report = header("timeout");
                report.addProperty("waitingFor", waitingFor);
                write(RESULTS, report);
            }
            if (Platform.getEnvironment() == Env.CLIENT) AutoTestClient.onWatchdog();
            Runtime.getRuntime().halt(3);
        }, "ModUtilities-AutoTest-Watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    private static void serverTick(MinecraftServer server) {
        if (finished) return;
        if (++tick == 1) ensureMultiServer(server);

        String waiting = waitingFor(server);
        if (waiting != null) {
            if (!waiting.equals(waitingFor)) ModUtilitiesMod.LOGGER.info("[AutoTest] waiting for {}", waiting);
            waitingFor = waiting;
            readyTicks = 0;
            return;
        }
        waitingFor = "settle";
        if (++readyTicks < SETTLE_TICKS) return;
        run(server);
    }

    private static @Nullable String waitingFor(MinecraftServer server) {
        if (WAIT_PLAYER && server.getPlayerList().getPlayers().isEmpty()) return "a player";
        MultiServerConfig cfg = MultiServerConfig.get();
        if (cfg.enable && !cfg.isMaster && !(MultiServerManager.isSlave() && MultiServerManager.isRunning()))
            return "the link to the master";
        if (cfg.enable && cfg.isMaster && MultiServerManager.getConnectedSlaveIDs().size() < WAIT_SLAVES)
            return WAIT_SLAVES + " connected slave(s)";
        if (AFTER != null && !Files.exists(Paths.get(AFTER))) return "file " + AFTER;
        return null;
    }

    /** ModUtilities alone never creates the multi-server link (BankSystem does); create it from the config then. */
    // ponytail: no-op callbacks; only the link state matters to the tests that run without BankSystem.
    private static void ensureMultiServer(MinecraftServer server) {
        MultiServerConfig cfg = MultiServerConfig.get();
        if (!cfg.enable || MultiServerManager.instanceExists()) return;
        ModUtilitiesMod.LOGGER.info("[AutoTest] no mod created the multi-server link, creating it ({})", cfg.isMaster ? "master" : "slave");
        boolean created = cfg.isMaster
                ? MultiServerManager.createMaster(server, cfg.sharedSecret, cfg.masterTcpPort, () -> {}, t -> {}, id -> {}, id -> {})
                : MultiServerManager.createSlave(server, cfg.sharedSecret, cfg.slaveID, cfg.masterHost, cfg.masterTcpPort, () -> {}, s -> {}, t -> {}, () -> {});
        if (created) {
            ownsMultiServer = true;
            MultiServerManager.start();
        }
    }

    private static void run(MinecraftServer server) {
        boolean isSlave = MultiServerManager.isSlave();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        ServerPlayer player = players.isEmpty() ? null : players.get(0);
        JsonObject report = header("completed");
        try {
            Set<String> filter = MODS == null ? null : new LinkedHashSet<>(Arrays.asList(MODS.split(",")));
            Set<String> modIds = new LinkedHashSet<>();
            for (TestSuite suite : TestRegistry.getTestSuites()) {
                String modId = suite.getCategory().getModId();
                if (filter == null || filter.contains(modId)) modIds.add(modId);
            }
            int[] total = new int[3];
            JsonObject mods = new JsonObject();
            for (String modId : modIds) {
                ModUtilitiesMod.LOGGER.info("[AutoTest] running {} tests (slave={})", modId, isSlave);
                TestRunner runner = new TestRunner(modId, modId, isSlave, server);
                runner.runAll(player);
                mods.add(modId, modReport(modId, isSlave, runner.getResults(), total));
            }
            report.add("mods", mods);
            report.addProperty("passed", total[0]);
            report.addProperty("failed", total[1]);
            report.addProperty("errors", total[2]);
        } catch (Throwable t) {
            ModUtilitiesMod.LOGGER.error("[AutoTest] run aborted", t);
            report.addProperty("status", "error");
            report.addProperty("error", t.toString());
        }
        write(RESULTS, report);
        finished = true;
        ModUtilitiesMod.LOGGER.info("[AutoTest] report written to {}", RESULTS);
        if (server.isDedicatedServer() && STOP) {
            server.halt(false);
        }
    }

    private static JsonObject modReport(String modId, boolean isSlave, Map<String, List<TestResult>> results, int[] total) {
        JsonObject mod = new JsonObject();
        int[] counts = new int[3];
        JsonObject suites = new JsonObject();
        for (Map.Entry<String, List<TestResult>> e : results.entrySet()) {
            JsonArray tests = new JsonArray();
            for (TestResult r : e.getValue()) {
                JsonObject t = new JsonObject();
                t.addProperty("name", r.getTestName());
                t.addProperty("status", r.getStatus().name());
                if (r.getMessage() != null) t.addProperty("message", r.getMessage());
                if (r.getExpected() != null) t.addProperty("expected", r.getExpected());
                if (r.getActual() != null) t.addProperty("actual", r.getActual());
                tests.add(t);
                counts[r.getStatus().ordinal()]++;
            }
            suites.add(e.getKey(), tests);
        }
        JsonArray skipped = new JsonArray();
        for (TestSuite suite : TestRegistry.getTestSuites(modId)) {
            if (!suite.getCategory().canRunOn(isSlave)) skipped.add(suite.getCategory().getName() + "/" + suite.getClass().getSimpleName());
        }
        mod.addProperty("passed", counts[0]);
        mod.addProperty("failed", counts[1]);
        mod.addProperty("errors", counts[2]);
        mod.add("skippedSuites", skipped);
        mod.add("suites", suites);
        for (int i = 0; i < 3; i++) total[i] += counts[i];
        return mod;
    }

    static JsonObject header(String status) {
        JsonObject o = new JsonObject();
        o.addProperty("instance", LABEL);
        o.addProperty("status", status);
        o.addProperty("role", MultiServerManager.isSlave() ? "slave" : MultiServerManager.isMaster() ? "master" : "standalone");
        o.addProperty("durationMs", System.currentTimeMillis() - START_MILLIS);
        return o;
    }

    /** Writes via a temp file + rename, so other instances polling for the file never see it half written. */
    static void write(Path file, JsonObject json) {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(json));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            ModUtilitiesMod.LOGGER.error("[AutoTest] could not write {}", file, e);
        }
    }
}
