package net.kroia.modutilities.testing;

import net.kroia.modutilities.ModUtilitiesMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class TestRunner {

    private final String prefix;
    private final String modId;
    private final boolean isSlave;
    private final @Nullable MinecraftServer server;

    /**
     * Plain-text report of the current run, flushed to {@link #logFile()} when the run ends.
     * StringBuffer, not StringBuilder: while a run is active the log4j capture appender
     * (see {@link #beginLog(String)}) feeds it from whatever thread emitted the log event.
     */
    private final StringBuffer log = new StringBuffer();

    /** Live log4j appender while a run is in progress, {@code null} otherwise. */
    private @Nullable Appender captureAppender;

    public TestRunner(String modName, String modId, boolean isSlave, @Nullable MinecraftServer server) {
        this.prefix = "[" + modName + " Test] ";
        this.modId = modId;
        this.isSlave = isSlave;
        this.server = server;
    }

    public void runAll(ServerPlayer player) {
        beginLog("all categories");
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

        beginLog("category " + category.getName());
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
            log.append("No test suites registered for category: ").append(categoryName).append('\n');
            flushLog();
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

        log.append('\n').append("--- ").append(category.getName())
                .append(" (").append(testCount).append(" tests) ---\n");

        int passed = 0;
        int failed = 0;
        int error = 0;

        List<TestResult> results = new ArrayList<>();

        // Everything from setup() onwards runs inside a try/finally so that teardown()
        // always gets a chance to release real world state (accounts, markets, registry
        // entries), including when setup() itself throws half-way through or when a test
        // throws an Error rather than an Exception.
        try {
            try {
                suite.setup();
            } catch (Throwable t) {
                logThrowable("Setup failed in " + category.getName(), t);
                log.append("SETUP FAILED: ").append(describe(t)).append('\n');
                player.sendSystemMessage(Component.literal("  ")
                        .append(Component.literal("Setup failed: " + describe(t)).withStyle(ChatFormatting.RED)));
                // Returning here still runs the finally block below, so a partially
                // completed setup gets cleaned up.
                return new int[]{0, 0, testCount};
            }

            for (Map.Entry<String, Supplier<TestResult>> entry : suite.getTests().entrySet()) {
                String testName = entry.getKey();
                Supplier<TestResult> testSupplier = entry.getValue();

                TestResult result;
                try {
                    result = testSupplier.get();
                    result = copyWithName(result, testName);
                } catch (Throwable t) {
                    // Catch Throwable, not Exception: a NoSuchMethodError / AssertionError
                    // from a single test must not escape and kill the server tick loop.
                    logThrowable("Test '" + testName + "' threw", t);
                    result = TestResult.error(testName, describe(t));
                }

                results.add(result);
                reportTestResult(player, result);
                logTestResult(result);

                switch (result.getStatus()) {
                    case PASS -> passed++;
                    case FAIL -> failed++;
                    case ERROR -> error++;
                }
            }
        } finally {
            try {
                suite.teardown();
            } catch (Throwable t) {
                // Teardown problems are reported but never propagate, so they cannot
                // mask a test result or abort the remaining suites.
                logThrowable("Teardown failed in " + category.getName(), t);
                log.append("TEARDOWN FAILED: ").append(describe(t)).append('\n');
                player.sendSystemMessage(Component.literal("  ")
                        .append(Component.literal("Teardown failed: " + describe(t)).withStyle(ChatFormatting.RED)));
            }
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

        log.append('\n').append("=== Summary ===\n")
                .append(passed).append(" passed, ")
                .append(failed).append(" failed, ")
                .append(error).append(" errors, ")
                .append(skipped).append(" skipped\n");
        flushLog();
    }

    /** {@code logs/UnitTests/<modId>.log} — one report file per mod using the test API. */
    private Path logFile() {
        return Paths.get("logs", "UnitTests", modId + ".log");
    }

    /**
     * Starts a fresh report: drops the previous run's file, writes a header, and attaches
     * a log4j appender to the root logger so that everything logged by any mod while the
     * run is in progress is interleaved into the report.
     *
     * @param scope what is being run, for the header line.
     */
    private void beginLog(String scope) {
        log.setLength(0);
        try {
            Files.deleteIfExists(logFile());
        } catch (IOException e) {
            ModUtilitiesMod.LOGGER.error("[TestRunner] Could not delete old test log {}", logFile(), e);
        }
        log.append(prefix.trim()).append(' ').append(scope).append(" @ ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append('\n');
        attachCaptureAppender();
    }

    /**
     * Attaches the capture appender to the log4j root logger. Any previously attached
     * appender is detached first, so an aborted run cannot leak one.
     * <p>
     * The appender is unfiltered by design: it mirrors whatever reaches the root logger,
     * which is exactly what makes ordinary log statements show up between test results.
     */
    private void attachCaptureAppender() {
        detachCaptureAppender();
        if (!(LogManager.getRootLogger() instanceof Logger root)) {
            // Not running on log4j-core (shouldn't happen in Minecraft) - results only.
            return;
        }
        PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("[%d{HH:mm:ss}] [%t/%level] [%logger{1}]: %msg%n%throwable")
                .build();
        Appender appender = new AbstractAppender("ModUtilitiesTestCapture-" + modId, null, layout,
                true, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                // One atomic append of the fully formatted line: concurrent log events from
                // other threads can arrive out of order, but never shred each other's text.
                log.append(layout.toSerializable(event));
            }
        };
        appender.start();
        root.addAppender(appender);
        captureAppender = appender;
    }

    private void detachCaptureAppender() {
        if (captureAppender == null) return;
        if (LogManager.getRootLogger() instanceof Logger root) {
            root.removeAppender(captureAppender);
        }
        captureAppender.stop();
        captureAppender = null;
    }

    private void logTestResult(TestResult result) {
        switch (result.getStatus()) {
            case PASS -> log.append("PASS ").append(result.getTestName()).append('\n');
            case FAIL -> {
                log.append("FAIL ").append(result.getTestName()).append('\n');
                if (result.getExpected() != null && result.getActual() != null) {
                    log.append("     Expected: ").append(result.getExpected())
                            .append(" Got: ").append(result.getActual()).append('\n');
                } else if (result.getMessage() != null) {
                    log.append("     ").append(result.getMessage()).append('\n');
                }
            }
            case ERROR -> {
                log.append("ERR  ").append(result.getTestName()).append('\n');
                if (result.getMessage() != null) {
                    log.append("     ").append(result.getMessage()).append('\n');
                }
            }
        }
    }

    /**
     * Detaches the capture appender and writes the accumulated report.
     * Failures are logged, never thrown at the caller.
     */
    private void flushLog() {
        detachCaptureAppender();
        Path file = logFile();
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, log);
        } catch (IOException e) {
            ModUtilitiesMod.LOGGER.error("[TestRunner] Could not write test log {}", file, e);
        }
    }

    /**
     * Builds a short, chat-safe description of a throwable.
     * Always includes the concrete class name, because {@code getMessage()} alone
     * renders as "null" for an NPE and similar message-less throwables.
     *
     * @param t the throwable to describe, must not be null.
     * @return "SimpleClassName: message", or just "SimpleClassName" when there is no message.
     */
    private static String describe(Throwable t) {
        String message = t.getMessage();
        String type = t.getClass().getSimpleName();
        return (message == null || message.isEmpty()) ? type : type + ": " + message;
    }

    /**
     * Logs the full stack trace to the server log. Stack traces are deliberately kept
     * out of player chat; only the short {@link #describe(Throwable)} form is sent there.
     *
     * @param context human readable description of where the throwable escaped from.
     * @param t       the throwable to log.
     */
    private static void logThrowable(String context, Throwable t) {
        ModUtilitiesMod.LOGGER.error("[TestRunner] {}: {}", context, describe(t), t);
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
