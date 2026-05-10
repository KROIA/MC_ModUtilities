package net.kroia.modutilities.testing;

import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class TestSuite {

    private final Map<String, Supplier<TestResult>> tests = new LinkedHashMap<>();
    private @Nullable MinecraftServer server;

    public abstract TestCategory getCategory();

    public abstract void registerTests();

    public void setup() {
    }

    public void teardown() {
    }

    protected void addTest(String name, Supplier<TestResult> test) {
        tests.put(name, test);
    }

    public Map<String, Supplier<TestResult>> getTests() {
        return tests;
    }

    public int getTestCount() {
        return tests.size();
    }

    public void setServer(@Nullable MinecraftServer server) {
        this.server = server;
    }

    protected @Nullable MinecraftServer getServer() {
        return server;
    }

    // ========================================================================
    // Assertion helpers
    // ========================================================================

    protected TestResult pass(String message) {
        return TestResult.pass("", message);
    }

    protected TestResult fail(String message) {
        return TestResult.fail("", message);
    }

    protected TestResult assertEquals(Object expected, Object actual) {
        if (expected == null && actual == null) {
            return pass("Both values are null");
        }
        if (expected != null && expected.equals(actual)) {
            return pass("Values match: " + expected);
        }
        return TestResult.fail("",
                "assertEquals failed",
                String.valueOf(expected),
                String.valueOf(actual));
    }

    protected TestResult assertEquals(String message, Object expected, Object actual) {
        if (expected == null && actual == null) {
            return pass(message);
        }
        if (expected != null && expected.equals(actual)) {
            return pass(message);
        }
        return TestResult.fail("",
                message,
                String.valueOf(expected),
                String.valueOf(actual));
    }

    protected TestResult assertTrue(boolean condition) {
        if (condition) {
            return pass("Condition is true");
        }
        return TestResult.fail("", "assertTrue failed", "true", "false");
    }

    protected TestResult assertTrue(String message, boolean condition) {
        if (condition) {
            return pass(message);
        }
        return TestResult.fail("", message, "true", "false");
    }

    protected TestResult assertFalse(boolean condition) {
        if (!condition) {
            return pass("Condition is false");
        }
        return TestResult.fail("", "assertFalse failed", "false", "true");
    }

    protected TestResult assertFalse(String message, boolean condition) {
        if (!condition) {
            return pass(message);
        }
        return TestResult.fail("", message, "false", "true");
    }

    protected TestResult assertNotNull(Object object) {
        if (object != null) {
            return pass("Object is not null");
        }
        return TestResult.fail("", "assertNotNull failed", "non-null", "null");
    }

    protected TestResult assertNotNull(String message, Object object) {
        if (object != null) {
            return pass(message);
        }
        return TestResult.fail("", message, "non-null", "null");
    }

    protected TestResult assertNull(Object object) {
        if (object == null) {
            return pass("Object is null");
        }
        return TestResult.fail("", "assertNull failed", "null", String.valueOf(object));
    }

    protected TestResult assertNull(String message, Object object) {
        if (object == null) {
            return pass(message);
        }
        return TestResult.fail("", message, "null", String.valueOf(object));
    }

    protected TestResult assertThrows(Class<? extends Throwable> expectedType, Runnable runnable) {
        try {
            runnable.run();
            return TestResult.fail("",
                    "assertThrows failed: no exception thrown",
                    expectedType.getSimpleName(),
                    "no exception");
        } catch (Throwable t) {
            if (expectedType.isInstance(t)) {
                return pass("Threw expected " + expectedType.getSimpleName());
            }
            return TestResult.fail("",
                    "assertThrows failed: wrong exception type",
                    expectedType.getSimpleName(),
                    t.getClass().getSimpleName());
        }
    }

    protected TestResult assertThrows(String message, Class<? extends Throwable> expectedType, Runnable runnable) {
        try {
            runnable.run();
            return TestResult.fail("",
                    message,
                    expectedType.getSimpleName(),
                    "no exception");
        } catch (Throwable t) {
            if (expectedType.isInstance(t)) {
                return pass(message);
            }
            return TestResult.fail("",
                    message,
                    expectedType.getSimpleName(),
                    t.getClass().getSimpleName());
        }
    }
}
