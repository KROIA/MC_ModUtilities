package net.kroia.modutilities.testing.tests;

import net.kroia.modutilities.networking.multi_server.MultiServerManager;
import net.kroia.modutilities.testing.TestCategory;
import net.kroia.modutilities.testing.TestSuite;
import net.kroia.modutilities.testing.autotest.AutoTest;
import net.kroia.modutilities.testing.categories.ModUtilitiesTestCategories;

/**
 * The slave-masterdown autotest cell (run_matrix.py): the master was killed while a client played on this slave.
 * Skips everywhere else.
 */
public class MasterDownTests extends TestSuite {
    @Override
    public TestCategory getCategory() {
        return ModUtilitiesTestCategories.MULTI_SERVER;
    }

    @Override
    public void registerTests() {
        addTest("masterdown_link_down_ticks_sampled", () -> {
            if (!AutoTest.isMasterDown())
                return skip("not the slave-masterdown cell");
            if (MultiServerManager.isRunning())
                return fail("MultiServerManager.isRunning() with the master down");
            AutoTest.TickStats t = AutoTest.tickStats();
            if (t.ticks() == 0)
                return fail("no server ticks sampled while the master was down");
            // ponytail: no tick threshold yet; the numbers are reported until a limit is agreed
            return pass(String.format("ticks %d, max %.1f ms, avg %.1f ms", t.ticks(), t.maxTickMs(), t.avgTickMs()));
        });
    }
}
