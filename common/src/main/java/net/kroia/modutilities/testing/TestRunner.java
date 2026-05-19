package net.kroia.modutilities.testing;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class TestRunner {

    private final String prefix;
    private final String modId;
    private final boolean isSlave;
    private final @Nullable MinecraftServer server;

    public TestRunner(String modName, String modId, boolean isSlave, @Nullable MinecraftServer server) {
        this.prefix = "[" + modName + " Test] ";
        this.modId = modId;
        this.isSlave = isSlave;
        this.server = server;
    }

    public void runAll(ServerPlayer player) {
        List<TestSuite> suites = TestRegistry.getTestSuites(modId);
        int totalPassed = 0;
        int totalFailed = 0;
        int totalError = 0;
        int totalSkipped = 0;

        for (TestSuite suite : suites) {
            TestCategory category = suite.getCategory();
            if (!category.canRunOn(isSlave)) {
                totalSkipped += suite.getTestCount();
                continue;
            }
            int[] counts = runSuite(suite, player);
            totalPassed += counts[0];
            totalFailed += counts[1];
            totalError += counts[2];
        }

        sendSummary(player, totalPassed, totalFailed, totalError, totalSkipped);
    }

    public void runCategory(ServerPlayer player, String categoryName) {
        TestCategory category = TestCategory.fromName(categoryName);
        if (category == null) {
            player.sendSystemMessage(Component.literal(prefix)
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("Unknown category: ").withStyle(ChatFormatting.RED))
                    .append(Component.literal(categoryName).withStyle(ChatFormatting.YELLOW)));
            return;
        }

        if (!category.canRunOn(isSlave)) {
            player.sendSystemMessage(Component.literal(prefix)
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("Category ").withStyle(ChatFormatting.RED))
                    .append(Component.literal(categoryName).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" cannot run on this server type").withStyle(ChatFormatting.RED)));
            return;
        }

        List<TestSuite> suites = TestRegistry.getTestSuites(modId);
        int totalPassed = 0;
        int totalFailed = 0;
        int totalError = 0;
        boolean found = false;

        for (TestSuite suite : suites) {
            if (suite.getCategory() == category) {
                found = true;
                int[] counts = runSuite(suite, player);
                totalPassed += counts[0];
                totalFailed += counts[1];
                totalError += counts[2];
            }
        }

        if (!found) {
            player.sendSystemMessage(Component.literal(prefix)
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("No test suites registered for category: ").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(categoryName).withStyle(ChatFormatting.AQUA)));
            return;
        }

        sendSummary(player, totalPassed, totalFailed, totalError, 0);
    }

    public void listCategories(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(prefix)
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Available test categories:").withStyle(ChatFormatting.WHITE)));

        List<TestSuite> suites = TestRegistry.getTestSuites(modId);

        for (TestCategory category : TestCategory.getAllCategories()) {
            if (!category.getModId().equals(modId)) continue;
            boolean canRun = category.canRunOn(isSlave);
            int testCount = 0;
            for (TestSuite suite : suites) {
                if (suite.getCategory() == category) {
                    if (suite.getTestCount() == 0) {
                        suite.registerTests();
                    }
                    testCount += suite.getTestCount();
                }
            }

            if (testCount == 0) {
                continue;
            }

            MutableComponent line = Component.literal("  ");
            if (canRun) {
                line.append(Component.literal(category.getName()).withStyle(ChatFormatting.AQUA));
            } else {
                line.append(Component.literal(category.getName()).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH));
            }
            line.append(Component.literal(" (" + testCount + " tests) ").withStyle(ChatFormatting.GRAY));
            line.append(Component.literal("- " + category.getDescription()).withStyle(ChatFormatting.DARK_GRAY));

            if (!canRun) {
                String reason = isSlave ? "master only" : "slave only";
                line.append(Component.literal(" [" + reason + "]").withStyle(ChatFormatting.DARK_RED));
            }

            player.sendSystemMessage(line);
        }
    }

    private int[] runSuite(TestSuite suite, ServerPlayer player) {
        if (suite.getTestCount() == 0) {
            suite.registerTests();
        }

        suite.setServer(server);

        TestCategory category = suite.getCategory();
        int testCount = suite.getTestCount();

        player.sendSystemMessage(Component.literal(prefix)
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Running: ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(category.getName()).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" (" + testCount + " tests)").withStyle(ChatFormatting.WHITE)));

        int passed = 0;
        int failed = 0;
        int error = 0;

        List<TestResult> results = new ArrayList<>();

        try {
            suite.setup();
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("  ")
                    .append(Component.literal("Setup failed: " + e.getMessage()).withStyle(ChatFormatting.RED)));
            return new int[]{0, 0, testCount};
        }

        for (Map.Entry<String, Supplier<TestResult>> entry : suite.getTests().entrySet()) {
            String testName = entry.getKey();
            Supplier<TestResult> testSupplier = entry.getValue();

            TestResult result;
            try {
                result = testSupplier.get();
                result = copyWithName(result, testName);
            } catch (Exception e) {
                result = TestResult.error(testName, "Exception: " + e.getMessage());
            }

            results.add(result);
            reportTestResult(player, result);

            switch (result.getStatus()) {
                case PASS -> passed++;
                case FAIL -> failed++;
                case ERROR -> error++;
            }
        }

        try {
            suite.teardown();
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("  ")
                    .append(Component.literal("Teardown failed: " + e.getMessage()).withStyle(ChatFormatting.RED)));
        }

        MutableComponent summaryLine = Component.literal(prefix)
                .withStyle(ChatFormatting.GOLD);
        summaryLine.append(Component.literal(category.getName()).withStyle(ChatFormatting.AQUA));
        summaryLine.append(Component.literal(": ").withStyle(ChatFormatting.WHITE));
        summaryLine.append(Component.literal(String.valueOf(passed)).withStyle(ChatFormatting.GREEN));
        summaryLine.append(Component.literal("/").withStyle(ChatFormatting.YELLOW));
        summaryLine.append(Component.literal(String.valueOf(testCount)).withStyle(ChatFormatting.YELLOW));
        summaryLine.append(Component.literal(" passed").withStyle(ChatFormatting.WHITE));
        if (failed > 0) {
            summaryLine.append(Component.literal(", ").withStyle(ChatFormatting.WHITE));
            summaryLine.append(Component.literal(String.valueOf(failed)).withStyle(ChatFormatting.RED));
            summaryLine.append(Component.literal(" failed").withStyle(ChatFormatting.WHITE));
        }
        if (error > 0) {
            summaryLine.append(Component.literal(", ").withStyle(ChatFormatting.WHITE));
            summaryLine.append(Component.literal(String.valueOf(error)).withStyle(ChatFormatting.YELLOW));
            summaryLine.append(Component.literal(" errors").withStyle(ChatFormatting.WHITE));
        }
        player.sendSystemMessage(summaryLine);

        return new int[]{passed, failed, error};
    }

    private void reportTestResult(ServerPlayer player, TestResult result) {
        switch (result.getStatus()) {
            case PASS -> {
                player.sendSystemMessage(Component.literal("  ")
                        .append(Component.literal("✓ " + result.getTestName()).withStyle(ChatFormatting.GREEN)));
            }
            case FAIL -> {
                player.sendSystemMessage(Component.literal("  ")
                        .append(Component.literal("✗ " + result.getTestName()).withStyle(ChatFormatting.RED)));
                if (result.getExpected() != null && result.getActual() != null) {
                    player.sendSystemMessage(Component.literal("    Expected: ")
                            .withStyle(ChatFormatting.RED)
                            .append(Component.literal(result.getExpected()).withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal(" Got: ").withStyle(ChatFormatting.RED))
                            .append(Component.literal(result.getActual()).withStyle(ChatFormatting.YELLOW)));
                } else if (result.getMessage() != null) {
                    player.sendSystemMessage(Component.literal("    " + result.getMessage()).withStyle(ChatFormatting.RED));
                }
            }
            case ERROR -> {
                player.sendSystemMessage(Component.literal("  ")
                        .append(Component.literal("! " + result.getTestName()).withStyle(ChatFormatting.YELLOW)));
                if (result.getMessage() != null) {
                    player.sendSystemMessage(Component.literal("    " + result.getMessage()).withStyle(ChatFormatting.YELLOW));
                }
            }
        }
    }

    private void sendSummary(ServerPlayer player, int passed, int failed, int error, int skipped) {
        player.sendSystemMessage(Component.literal(""));
        MutableComponent summary = Component.literal(prefix)
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal("=== Summary === ").withStyle(ChatFormatting.WHITE));
        player.sendSystemMessage(summary);

        MutableComponent counts = Component.literal(prefix)
                .withStyle(ChatFormatting.GOLD);
        counts.append(Component.literal(String.valueOf(passed)).withStyle(ChatFormatting.GREEN));
        counts.append(Component.literal(" passed").withStyle(ChatFormatting.WHITE));
        if (failed > 0) {
            counts.append(Component.literal(", ").withStyle(ChatFormatting.WHITE));
            counts.append(Component.literal(String.valueOf(failed)).withStyle(ChatFormatting.RED));
            counts.append(Component.literal(" failed").withStyle(ChatFormatting.WHITE));
        }
        if (error > 0) {
            counts.append(Component.literal(", ").withStyle(ChatFormatting.WHITE));
            counts.append(Component.literal(String.valueOf(error)).withStyle(ChatFormatting.YELLOW));
            counts.append(Component.literal(" errors").withStyle(ChatFormatting.WHITE));
        }
        if (skipped > 0) {
            counts.append(Component.literal(", ").withStyle(ChatFormatting.WHITE));
            counts.append(Component.literal(String.valueOf(skipped)).withStyle(ChatFormatting.DARK_GRAY));
            counts.append(Component.literal(" skipped").withStyle(ChatFormatting.WHITE));
        }
        player.sendSystemMessage(counts);

        int total = passed + failed + error;
        if (total > 0 && failed == 0 && error == 0) {
            player.sendSystemMessage(Component.literal(prefix)
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("All tests passed!").withStyle(ChatFormatting.GREEN)));
        }
    }

    private static TestResult copyWithName(TestResult source, String name) {
        return switch (source.getStatus()) {
            case PASS -> TestResult.pass(name, source.getMessage());
            case FAIL -> {
                if (source.getExpected() != null && source.getActual() != null) {
                    yield TestResult.fail(name, source.getMessage(), source.getExpected(), source.getActual());
                }
                yield TestResult.fail(name, source.getMessage());
            }
            case ERROR -> TestResult.error(name, source.getMessage() != null ? source.getMessage() : "");
        };
    }
}
