package net.kroia.modutilities.testing;

import net.kroia.modutilities.sandbox.Sandbox;
import net.kroia.modutilities.testing.autotest.AutoTest;

/**
 * The only way release code reaches dev-only code (sandbox, unit tests, autotest).
 * <p>
 * Release builds ({@code enable_dev_features=false} in gradle.properties) do not compile the
 * {@code testing} and {@code sandbox} packages at all, so this class does not exist there.
 * {@link net.kroia.modutilities.ModUtilitiesMod} loads it reflectively, which keeps release
 * code compiling without it.
 */
public final class DevBootstrap {
    private DevBootstrap() {}

    /** Called once from {@code ModUtilitiesMod.init()} when dev features are enabled. */
    public static void init() {
        Sandbox.init();
        AutoTest.init();
    }
}
