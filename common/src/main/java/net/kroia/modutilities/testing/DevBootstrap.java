package net.kroia.modutilities.testing;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.networking.multi_server.MultiServerConfig;
import net.kroia.modutilities.networking.multi_server.MultiServerManager;
import net.kroia.modutilities.sandbox.Sandbox;
import net.kroia.testkit.autotest.AutoTest;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

/**
 * The only way release code reaches dev-only code (sandbox, unit tests, autotest).
 * <p>
 * Release builds ({@code enable_dev_features=false} in gradle.properties) do not compile the
 * {@code testing} and {@code sandbox} packages at all, so this class does not exist there.
 * {@link net.kroia.modutilities.ModUtilitiesMod} loads it reflectively, and only when the dev-only
 * TestKit mod is loaded, which keeps release code compiling without it.
 */
public final class DevBootstrap {
    private DevBootstrap() {}

    private static final String P = "modutilities.autotest";
    /** Master: wait for this many connected slaves (default 0). */
    private static final int WAIT_SLAVES = Integer.getInteger(P + ".waitSlaves", 0);
    /** Slave: wait for the link to the master (default true; false for the slave-masterdown cell). */
    private static final boolean WAIT_LINK = Boolean.parseBoolean(System.getProperty(P + ".waitLink", "true"));

    private static boolean firstTickDone;
    private static boolean ownsMultiServer;

    /** Called once from {@code ModUtilitiesMod.init()} when dev features are enabled and TestKit is loaded. */
    public static void init() {
        Sandbox.init();
        SimulatedMasterHang.init();
        if (AutoTest.isEnabled()) initAutoTest();
    }

    /**
     * The multi-server part of TestKit's AutoTest. The role comes from the instance's
     * {@code config/modutilities/MultiServerConfig.json}; if that config is enabled but no mod created the link by
     * the first server tick (ModUtilities running alone), it is created here (also in play mode).
     */
    private static void initAutoTest() {
        TickEvent.SERVER_POST.register(server -> {
            if (firstTickDone) return;
            firstTickDone = true;
            ensureMultiServer(server);
        });
        LifecycleEvent.SERVER_STOPPING.register(server -> {
            if (ownsMultiServer) MultiServerManager.cleanup();
        });
        AutoTest.addReadyCondition(server -> {
            MultiServerConfig cfg = MultiServerConfig.get();
            if (WAIT_LINK && cfg.enable && !cfg.isMaster && !(MultiServerManager.isSlave() && MultiServerManager.isRunning()))
                return "the link to the master";
            if (cfg.enable && cfg.isMaster && MultiServerManager.getConnectedSlaveIDs().size() < WAIT_SLAVES)
                return WAIT_SLAVES + " connected slave(s)";
            return null;
        });
        AutoTest.setRoleProvider(() -> MultiServerManager.isSlave() ? "slave" : MultiServerManager.isMaster() ? "master" : "standalone");
        if (Platform.getEnvironment() == Env.CLIENT) {
            AutoTest.onClientJoin(() -> offscreenGuiCheck(Minecraft.getInstance()));
        }
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

    /**
     * Client only. Builds and closes the GuiRenderer display blocks render into (1.21.6+), with the game's own
     * GuiRenderer already alive. Throws (crashing the run) if a second GuiRenderer cannot be built, as with Fabric API 0.129.
     */
    private static void offscreenGuiCheck(Minecraft mc) {
        //new net.kroia.modutilities.gui.client.OffscreenGuiRenderer(mc.renderBuffers().bufferSource()).close(); // mc>=1.21.6 mc<=26.1
        //new net.kroia.modutilities.gui.client.OffscreenGuiRenderer().close(); // mc>=26.2
    }
}
