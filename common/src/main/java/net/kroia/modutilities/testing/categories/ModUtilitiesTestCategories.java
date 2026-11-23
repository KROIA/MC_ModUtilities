package net.kroia.modutilities.testing.categories;

import net.kroia.testkit.TestCategory;

public class ModUtilitiesTestCategories {
    public static final TestCategory NETWORKING = new TestCategory(
        "modutilities", "mu_networking", "NetworkPacketManager and ARRS tests",
        TestCategory.ServerType.BOTH, true);
    public static final TestCategory STREAMING = new TestCategory(
        "modutilities", "mu_streaming", "StreamSystem tests",
        TestCategory.ServerType.BOTH, true);
    public static final TestCategory SETTINGS = new TestCategory(
        "modutilities", "mu_settings", "ModSettings save/load tests",
        TestCategory.ServerType.MASTER_ONLY, false);
    public static final TestCategory PERSISTENCE = new TestCategory(
        "modutilities", "mu_persistence", "NBT/JSON persistence tests",
        TestCategory.ServerType.MASTER_ONLY, false);
    public static final TestCategory EVENTS = new TestCategory(
        "modutilities", "mu_events", "DataEvent and Signal tests",
        TestCategory.ServerType.BOTH, false);
    public static final TestCategory MULTI_SERVER = new TestCategory(
        "modutilities", "mu_multi_server", "Master/slave communication tests",
        TestCategory.ServerType.BOTH, true);
    public static final TestCategory GUI = new TestCategory(
        "modutilities", "mu_gui", "GUI element logic tests",
        TestCategory.ServerType.BOTH, false);
    public static final TestCategory UTILITIES = new TestCategory(
        "modutilities", "mu_utilities", "ColorUtilities, JsonUtilities, TimerMillis, and ItemUtilities helper tests",
        TestCategory.ServerType.BOTH, false);
    public static final TestCategory LUCKPERMS = new TestCategory(
        "modutilities", "mu_luckperms", "LuckPermsBridge with LuckPerms installed, and worlds after LuckPerms was removed",
        TestCategory.ServerType.BOTH, true);
    public static final TestCategory DISCORD = new TestCategory(
        "modutilities", "mu_discord", "DiscordWebhooks config, rendering, batching and rate limits (fake HTTP)",
        TestCategory.ServerType.BOTH, false);
    public static final TestCategory PLACEHOLDER = new TestCategory(
        "modutilities", "mu_placeholder", "PlaceholderRegistry resolved through Placeholder API (Fabric)",
        TestCategory.ServerType.BOTH, true);
    public static final TestCategory COMMANDS = new TestCategory(
        "modutilities", "mu_commands", "Commands run through TestKit's CommandRunner, replies checked",
        TestCategory.ServerType.BOTH, true);
}
