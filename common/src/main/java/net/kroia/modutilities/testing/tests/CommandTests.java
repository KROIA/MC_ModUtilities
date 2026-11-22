package net.kroia.modutilities.testing.tests;

import net.kroia.modutilities.testing.categories.ModUtilitiesTestCategories;
import net.kroia.testkit.CommandRunner;
import net.kroia.testkit.CommandRunner.Reply;
import net.kroia.testkit.TestCategory;
import net.kroia.testkit.TestResult;
import net.kroia.testkit.TestSuite;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.List;

/**
 * Commands run through TestKit's {@link CommandRunner}, with their replies checked: as console, as a player with a
 * forced permission level, a syntax error under {@code /modutilities}, and the read-only
 * {@code /modutilities discord status}. The player test skips when nobody is online (dedicated servers).
 */
public class CommandTests extends TestSuite {

    @Override
    public TestCategory getCategory() {
        return ModUtilitiesTestCategories.COMMANDS;
    }

    @Override
    public void registerTests() {
        addTest("cmd_console_seed", this::testConsoleSeed);
        addTest("cmd_player_permission_level", this::testPlayerPermissionLevel);
        addTest("cmd_syntax_error_fails_with_message", this::testSyntaxError);
        addTest("cmd_discord_status_reply", this::testDiscordStatus);
    }

    private TestResult testConsoleSeed() {
        if (getServer() == null) return skip("no server");
        Reply r = CommandRunner.asConsole(getServer(), "seed");
        return assertTrue(r.toString(), r.success() && r.contains("Seed"));
    }

    private TestResult testPlayerPermissionLevel() {
        MinecraftServer server = getServer();
        if (server == null) return skip("no server");
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return skip("no online player");
        ServerPlayer player = players.get(0);
        GameType before = player.gameMode.getGameModeForPlayer();
        // A mode the player is not in, so a successful command really changes something.
        GameType target = before == GameType.CREATIVE ? GameType.SURVIVAL : GameType.CREATIVE;
        String cmd = "gamemode " + target.getName();
        try {
            Reply denied = CommandRunner.asPlayer(player, 0, cmd);
            if (denied.success() || player.gameMode.getGameModeForPlayer() != before)
                return fail("level 0 must not run /gamemode: " + denied);
            Reply allowed = CommandRunner.asPlayer(player, 2, cmd);
            return assertTrue("level 2 must run /gamemode: " + allowed,
                    allowed.success() && player.gameMode.getGameModeForPlayer() == target);
        } finally {
            player.setGameMode(before);
        }
    }

    private TestResult testSyntaxError() {
        if (getServer() == null) return skip("no server");
        Reply r = CommandRunner.asConsole(getServer(), "modutilities nosuchsubcommand");
        return assertTrue(r.toString(), !r.success() && !r.text().isEmpty());
    }

    private TestResult testDiscordStatus() {
        if (getServer() == null) return skip("no server");
        Reply r = CommandRunner.asConsole(getServer(), "modutilities discord status");
        return assertTrue(r.toString(), r.success() && r.contains("Discord: "));
    }
}
