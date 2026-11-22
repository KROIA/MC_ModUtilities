package net.kroia.modutilities.testing.fabric;

import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.ServerPlaceholderContext; // mc>=26.1 (Placeholder API 3)
import net.fabricmc.loader.api.FabricLoader;
import net.kroia.modutilities.placeholder.PlaceholderRegistry;
import net.kroia.testkit.TestCategory;
import net.kroia.testkit.TestResult;
import net.kroia.testkit.TestSuite;
import net.kroia.modutilities.testing.categories.ModUtilitiesTestCategories;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * MU placeholders resolved through Patbox's Text Placeholder API (Fabric dev runtime only, see fabric/build.gradle).
 * Lives in the fabric source set (dev-only: the testing package is excluded from release builds) and is registered
 * reflectively by {@code Sandbox.registerTests}, so NeoForge never sees it. Skips when "placeholder-api" is not loaded.
 * <p>
 * The server placeholder is registered at mod init, before {@code PlaceholderApiBackend.attach()} (replay path); the
 * player placeholder on the first run, after it (forward path). PlaceholderRegistry has no unregister, so both stay
 * registered for the JVM lifetime; the {@code test_papi_} names keep them apart from real placeholders.
 */
public class PlaceholderApiTests extends TestSuite {
    private static final String NS = "modutilities";
    private static final String SERVER_NAME = "test_papi_server";
    private static final String PLAYER_NAME = "test_papi_player";
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("placeholder-api");
    private boolean playerRegistered;

    public PlaceholderApiTests() {
        if (LOADED)
            PlaceholderRegistry.registerServer(NS, SERVER_NAME, 1000, () -> "server_ok");
    }

    @Override
    public TestCategory getCategory() {
        return ModUtilitiesTestCategories.PLACEHOLDER;
    }

    @Override
    public void registerTests() {
        addTest("papi_server_placeholder_resolves", () -> {
            if (!LOADED)
                return skip("placeholder-api not loaded");
            MinecraftServer s = getServer();
            if (s == null)
                return skip("no server");
            return assertEquals("%" + NS + ":" + SERVER_NAME + "% via Placeholders.parseText",
                    "[server_ok]", Api.parse("[%" + NS + ":" + SERVER_NAME + "%]", s, null));
        });
        addTest("papi_player_placeholder_resolves", () -> {
            if (!LOADED)
                return skip("placeholder-api not loaded");
            MinecraftServer s = getServer();
            List<ServerPlayer> players = s == null ? List.of() : s.getPlayerList().getPlayers();
            if (players.isEmpty())
                return skip("no player online");
            if (!playerRegistered) {
                playerRegistered = true;
                PlaceholderRegistry.registerPlayer(NS, PLAYER_NAME, 1000, p -> "pl_" + p.getStringUUID());
            }
            ServerPlayer p = players.get(0);
            return assertEquals("%" + NS + ":" + PLAYER_NAME + "% via Placeholders.parseText",
                    "[pl_" + p.getStringUUID() + "]", Api.parse("[%" + NS + ":" + PLAYER_NAME + "%]", s, p));
        });
    }

    /** All Patbox references; only class-loaded once a test saw "placeholder-api" loaded. */
    private static final class Api {
        static String parse(String text, MinecraftServer server, ServerPlayer player) {
            //PlaceholderContext ctx = player != null ? PlaceholderContext.of(player) : PlaceholderContext.of(server); // mc<=1.21.11
            //return Placeholders.parseText(Component.literal(text), ctx).getString(); // mc<=1.21.11
            ServerPlaceholderContext ctx = player != null ? ServerPlaceholderContext.of(player) : ServerPlaceholderContext.of(server); // mc>=26.1 (Placeholder API 3)
            return Placeholders.SERVER_PLACEHOLDER_PARSER.parseComponent(text, ctx.asParserContext()).getString(); // mc>=26.1 (Placeholder API 3)
        }
    }
}
