package net.kroia.modutilities.testing;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicTest;
import org.opentest4j.AssertionFailedError;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Runs the registered {@link TestSuite}s that need no game ({@link TestSuite#needsGame()} == false) as JUnit tests,
 * without a Minecraft server or mod loader. A mod's {@code common/src/test} uses it from one {@code @TestFactory}:
 * <pre>{@code
 * @TestFactory Stream<DynamicTest> offline() { return OfflineTests.run(() -> MyModTestRegistration.register()); }
 * }</pre>
 * Same order as the in-game {@link TestRunner}: setup, tests, teardown (always), each one JUnit test named
 * {@code category/SuiteClass/test} (also {@code .../<setup>}, {@code .../<teardown>}); tests are skipped when setup failed.
 * SKIP results are reported as JUnit skips, FAIL/ERROR as failures.
 * <p>
 * No game bootstrap here (it costs ~4.5 s): a suite that touches registries (Items, ItemStack, registry codecs) calls
 * {@code SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();} in its setup(), a no-op in game. Code that reaches
 * {@code @ExpectPlatform} methods (ModChecker, Architectury Platform) or UtilitiesPlatform fails offline.
 */
public final class OfflineTests {
    private OfflineTests() {}

    /** @param registerSuites registers the mod's suites in {@link TestRegistry} (the same call the mod's init makes). */
    public static Stream<DynamicTest> run(Runnable registerSuites) {
        registerSuites.run();
        return TestRegistry.getTestSuites().stream()
                .filter(suite -> !suite.needsGame())
                .flatMap(OfflineTests::tests);
    }

    private static Stream<DynamicTest> tests(TestSuite suite) {
        if (suite.getTestCount() == 0) suite.registerTests();
        suite.setServer(null);
        String prefix = suite.getCategory().getName() + "/" + suite.getClass().getSimpleName() + "/";
        boolean[] setupOk = {false};
        List<DynamicTest> tests = new ArrayList<>();
        tests.add(DynamicTest.dynamicTest(prefix + "<setup>", () -> {
            suite.setup();
            setupOk[0] = true;
        }));
        suite.getTests().forEach((name, test) -> tests.add(DynamicTest.dynamicTest(prefix + name, () -> {
            Assumptions.assumeTrue(setupOk[0], "setup failed");
            check(test.get());
        })));
        tests.add(DynamicTest.dynamicTest(prefix + "<teardown>", suite::teardown));
        return tests.stream();
    }

    private static void check(TestResult result) {
        switch (result.getStatus()) {
            case PASS -> {}
            case SKIP -> Assumptions.abort(result.getMessage());
            case FAIL, ERROR -> {
                if (result.getExpected() != null || result.getActual() != null)
                    throw new AssertionFailedError(result.getMessage(), result.getExpected(), result.getActual());
                throw new AssertionFailedError(result.getStatus() + ": " + result.getMessage());
            }
        }
    }
}
