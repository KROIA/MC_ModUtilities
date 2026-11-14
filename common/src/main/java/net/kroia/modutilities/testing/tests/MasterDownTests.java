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
            String nums = String.format("ticks %d, max %.1f ms, avg %.1f ms", t.ticks(), t.maxTickMs(), t.avgTickMs());
            if (t.ticks() == 0)
                return fail("no server ticks sampled while the master was down");
            // Same limits as BankSystem's SlaveMasterDownTests; every cell so far stayed below max 85 ms, avg 50 ms.
            if (t.maxTickMs() >= 250)
                return fail("tick spike with the master down: " + nums);
            if (t.avgTickMs() >= 60)
                return fail("server lagging with the master down: " + nums);
            return pass(nums);
        });
    }
}
