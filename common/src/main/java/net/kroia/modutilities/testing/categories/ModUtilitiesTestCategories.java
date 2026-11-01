package net.kroia.modutilities.testing.categories;

import net.kroia.modutilities.testing.TestCategory;

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
}
