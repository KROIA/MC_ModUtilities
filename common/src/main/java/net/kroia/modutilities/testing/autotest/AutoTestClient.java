package net.kroia.modutilities.testing.autotest;

import com.google.gson.JsonObject;
import dev.architectury.event.events.client.ClientTickEvent;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.gui.client.ClientCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.nio.file.Path;

/**
 * Client half of {@link AutoTest}; only class-loaded on the physical client.
 * <p>
 * On the title screen it either joins {@code -Dmodutilities.autotest.connect=host:port} or creates a
 * fresh singleplayer world ({@code -Dmodutilities.autotest.world=new}, fixed seed). It quits when the
 * integrated server finished its run, when the dedicated server closes the connection after its run,
 * or when the connection fails. Its own status goes to {@code <results>.client.json}.
 */
final class AutoTestClient {
    private AutoTestClient() {}

    private static final String CONNECT = System.getProperty(AutoTest.P + ".connect");
    private static final String WORLD = System.getProperty(AutoTest.P + ".world");
    private static final long SEED = 20240101L;
    /** Ticks to stay on the title screen first, so late title-screen init (toasts, realms checks) is done. */
    private static final int TITLE_TICKS = 20;
    /** Time {@link Minecraft#stop()} gets to exit the JVM normally before it is halted. */
    private static final long EXIT_GRACE_MS = 30_000L;
    private static final Path CLIENT_RESULTS = AutoTest.RESULTS.resolveSibling(AutoTest.RESULTS.getFileName() + ".client.json");

    private static boolean started;
    private static boolean joined;
    private static boolean done;
    private static int titleTicks;

    static void init() {
        if (CONNECT == null && WORLD == null) {
            ModUtilitiesMod.LOGGER.warn("[AutoTest] client: neither .connect nor .world is set, staying idle");
            return;
        }
        ClientTickEvent.CLIENT_POST.register(AutoTestClient::tick);
    }

    private static void tick(Minecraft mc) {
        if (done) return;
        Screen screen = ClientCompat.screen();
        if (!started) {
            if (screen instanceof TitleScreen && ++titleTicks >= TITLE_TICKS) {
                started = true;
                start(mc, screen);
            }
            return;
        }
        if (mc.level != null) joined = true;
        if (AutoTest.isFinished()) {
            finish(mc, "tests_finished");
        } else if (joined && mc.level == null && !mc.isLocalServer()) {
            // A dedicated server under autotest stops itself after its run, which drops us here.
            finish(mc, "left_world");
        } else if (!joined && screen instanceof DisconnectedScreen) {
            finish(mc, "connect_failed");
        }
    }

    private static void start(Minecraft mc, Screen parent) {
        if (CONNECT != null) {
            ModUtilitiesMod.LOGGER.info("[AutoTest] client: connecting to {}", CONNECT);
            ConnectScreen.startConnecting(parent, mc, ServerAddress.parseString(CONNECT),
                    new ServerData("autotest", CONNECT, ServerData.Type.OTHER), false, null);
        } else {
            ModUtilitiesMod.LOGGER.info("[AutoTest] client: creating singleplayer world '{}'", WORLD);
            // DEMO_SETTINGS + a method reference keep this call identical on every supported version
            // (LevelSettings/GameRules constructors and the dimensions function type all drift).
            mc.createWorldOpenFlows().createFreshLevel("autotest-" + WORLD, MinecraftServer.DEMO_SETTINGS,
                    new WorldOptions(SEED, false, false), WorldPresets::createNormalWorldDimensions, parent);
        }
    }

    private static void finish(Minecraft mc, String status) {
        done = true;
        write(status);
        ModUtilitiesMod.LOGGER.info("[AutoTest] client: {}, quitting", status);
        // Dev launches do not always exit after Minecraft.stop() (non-daemon launcher thread pools;
        // NeoForge 26.x fails in its shutdown watchdog after closing the game module), so halt as a fallback.
        Thread exit = new Thread(() -> {
            try {
                Thread.sleep(EXIT_GRACE_MS);
            } catch (InterruptedException e) {
                return;
            }
            Runtime.getRuntime().halt(0);
        }, "ModUtilities-AutoTest-Exit");
        exit.setDaemon(true);
        exit.start();
        mc.stop();
    }

    private static void write(String status) {
        JsonObject o = new JsonObject();
        o.addProperty("instance", AutoTest.LABEL);
        o.addProperty("status", status);
        o.addProperty("joined", joined);
        AutoTest.write(CLIENT_RESULTS, o);
    }

    /** Called from the watchdog thread right before the JVM halts. */
    static void onWatchdog() {
        if (!done) write(started ? "timeout" : "timeout_before_title_screen");
    }
}
