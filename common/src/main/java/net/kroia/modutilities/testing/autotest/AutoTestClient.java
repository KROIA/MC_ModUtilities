package net.kroia.modutilities.testing.autotest;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.kroia.modutilities.ModUtilitiesMod;
import net.kroia.modutilities.gui.client.ClientCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DatapackLoadFailureScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;

/**
 * Client half of {@link AutoTest}; only class-loaded on the physical client.
 * <p>
 * On the title screen it either joins {@code -Dmodutilities.autotest.connect=host:port}, creates a
 * fresh superflat singleplayer world ({@code -Dmodutilities.autotest.world=new}, fixed seed) or opens the
 * existing save {@code saves/<name>} ({@code world=<name>}). It quits when the integrated server finished
 * its run, when the dedicated server closes the connection after its run, or when the connection fails.
 * Its own status goes to {@code <results>.client.json}. In play mode it only joins/opens and stays.
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
    /** Shown by NeoForge (>= 21.7 in dev: Architectury's @OnlyIn warning) before the title screen. */
    private static final String NEOFORGE_LOADING_SCREEN = "net.neoforged.neoforge.client.gui.LoadingErrorScreen";
    private static final Path CLIENT_RESULTS = AutoTest.RESULTS.resolveSibling(AutoTest.RESULTS.getFileName() + ".client.json");

    private static boolean started;
    private static boolean joined;
    private static boolean done;
    private static boolean warningsDismissed;
    private static int titleTicks;
    /** The last world-open prompt answered: it can stay on screen for a few ticks after the answer. */
    private static Screen answered;

    static void init() {
        if (CONNECT == null && WORLD == null) {
            ModUtilitiesMod.LOGGER.warn("[AutoTest] client: neither .connect nor .world is set, staying idle");
            return;
        }
        ClientTickEvent.CLIENT_POST.register(AutoTestClient::tick);
        // The user closes the game: make sure the dev JVM really exits (see finish()).
        if (AutoTest.PLAY) ClientLifecycleEvent.CLIENT_STOPPING.register(mc -> haltLater());
    }

    private static void tick(Minecraft mc) {
        if (done) return;
        Screen screen = ClientCompat.screen();
        if (!started) {
            if (!warningsDismissed && screen != null && screen.getClass().getName().equals(NEOFORGE_LOADING_SCREEN)) {
                dismissLoadingScreen(mc, screen);
                return;
            }
            if (screen instanceof TitleScreen && ++titleTicks >= TITLE_TICKS) {
                started = true;
                start(mc, screen);
                if (AutoTest.PLAY) done = true; // manual play: no quit logic
            }
            return;
        }
        if (mc.level != null && !joined) {
            joined = true;
            offscreenGuiCheck(mc);
        }
        if (AutoTest.isFinished()) {
            finish(mc, "tests_finished");
        } else if (joined && mc.level == null && !mc.isLocalServer()) {
            // A dedicated server under autotest stops itself after its run, which drops us here.
            finish(mc, "left_world");
        } else if (!joined && screen instanceof DisconnectedScreen) {
            finish(mc, "connect_failed");
        } else if (!joined && CONNECT == null && screen != null && screen != answered) {
            answerWorldPrompt(mc, screen);
        }
    }

    /**
     * Opening a save may stop at a prompt: a backup question (world from an older version that needs file
     * fixing on 26.x, snapshot upgrade, experimental/customized settings) or a yes/no (26.x "upgrade done, join
     * now?", low disk space). The save is a throwaway copy, so answer "join without backup" / "yes". A world
     * that cannot be opened (incompatible, broken datapacks) ends the run instead of waiting for the watchdog.
     * Worlds newer than the game never get here: run_matrix refuses them.
     */
    private static void answerWorldPrompt(Minecraft mc, Screen screen) {
        if (screen instanceof AlertScreen || screen instanceof DatapackLoadFailureScreen) {
            ModUtilitiesMod.LOGGER.error("[AutoTest] client: cannot open the world: {}", screen.getTitle().getString());
            finish(mc, "world_open_failed");
            return;
        }
        if (!(screen instanceof BackupConfirmScreen) && !(screen instanceof ConfirmScreen)) return;
        answered = screen;
        ModUtilitiesMod.LOGGER.info("[AutoTest] client: confirming '{}'", screen.getTitle().getString());
        try {
            if (screen instanceof BackupConfirmScreen) {
                ((BackupConfirmScreen.Listener) field(BackupConfirmScreen.class, "onProceed").get(screen)).proceed(false, false);
            } else {
                ((BooleanConsumer) field(ConfirmScreen.class, "callback").get(screen)).accept(true);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            ModUtilitiesMod.LOGGER.error("[AutoTest] client: cannot answer the world prompt", e);
            finish(mc, "world_prompt_failed");
        }
    }

    /**
     * Builds and closes the GuiRenderer display blocks render into (1.21.6+), with the game's own GuiRenderer
     * already alive. Throws (crashing the run) if a second GuiRenderer cannot be built, as with Fabric API 0.129.
     */
    private static void offscreenGuiCheck(Minecraft mc) {
        //new net.kroia.modutilities.gui.client.OffscreenGuiRenderer(mc.renderBuffers().bufferSource()).close(); // mc>=1.21.6 mc<=26.1
        //new net.kroia.modutilities.gui.client.OffscreenGuiRenderer().close(); // mc>=26.2
    }

    private static void start(Minecraft mc, Screen parent) {
        if (CONNECT != null) {
            ModUtilitiesMod.LOGGER.info("[AutoTest] client: connecting to {}", CONNECT);
            ConnectScreen.startConnecting(parent, mc, ServerAddress.parseString(CONNECT),
                    new ServerData("autotest", CONNECT, ServerData.Type.OTHER), false, null);
        } else if (!"new".equals(WORLD)) {
            ModUtilitiesMod.LOGGER.info("[AutoTest] client: opening saved world '{}'", WORLD);
            mc.createWorldOpenFlows().openWorld(WORLD, () -> {});
        } else {
            ModUtilitiesMod.LOGGER.info("[AutoTest] client: creating superflat singleplayer world '{}'", WORLD);
            // DEMO_SETTINGS + a method reference keep this call identical on every supported version
            // (LevelSettings/GameRules constructors and the dimensions function type all drift).
            mc.createWorldOpenFlows().createFreshLevel("autotest-" + WORLD, MinecraftServer.DEMO_SETTINGS,
                    new WorldOptions(SEED, false, false), AutoTestClient::flatWorld, parent);
        }
    }

    /**
     * The superflat preset from the registry (fast to generate). Takes {@link HolderLookup.Provider}, so the
     * method reference fits {@code createFreshLevel} on 1.21.1 ({@code RegistryAccess}) and later versions.
     */
    private static WorldDimensions flatWorld(HolderLookup.Provider registries) {
        return registries.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions();
    }

    /** Presses "Proceed" on a warnings-only NeoForge loading screen; fails fast on real errors. */
    private static void dismissLoadingScreen(Minecraft mc, Screen screen) {
        List<?> errors;
        List<?> warnings;
        Runnable next;
        try {
            Class<?> c = screen.getClass();
            errors = (List<?>) field(c, "modLoadErrors").get(screen);
            warnings = (List<?>) field(c, "modLoadWarnings").get(screen);
            next = (Runnable) field(c, "nextScreenTask").get(screen);
        } catch (ReflectiveOperationException | RuntimeException e) {
            ModUtilitiesMod.LOGGER.error("[AutoTest] client: cannot inspect NeoForge loading screen", e);
            finish(mc, "loading_errors");
            return;
        }
        if (!errors.isEmpty()) {
            ModUtilitiesMod.LOGGER.error("[AutoTest] client: NeoForge mod loading errors: {}", errors);
            finish(mc, "loading_errors");
            return;
        }
        warningsDismissed = true;
        ModUtilitiesMod.LOGGER.info("[AutoTest] client: dismissing NeoForge loading warnings ({})", warnings.size());
        next.run();
    }

    private static Field field(Class<?> c, String name) throws NoSuchFieldException {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private static void finish(Minecraft mc, String status) {
        done = true;
        write(status);
        ModUtilitiesMod.LOGGER.info("[AutoTest] client: {}, quitting", status);
        haltLater();
        mc.stop();
    }

    /**
     * Dev launches do not always exit after Minecraft.stop() (non-daemon launcher thread pools;
     * NeoForge 26.x fails in its shutdown watchdog after closing the game module), so halt as a fallback.
     */
    private static void haltLater() {
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
