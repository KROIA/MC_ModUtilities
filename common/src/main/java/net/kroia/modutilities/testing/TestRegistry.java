package net.kroia.modutilities.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestRegistry {

    /**
     * Master switch for the entire test framework.
     * Set to false for production/release builds to disable all test functionality.
     * When false:
     *   - Test suites are not registered
     *   - Test commands are not registered
     *   - No test code is executed
     */
    public static final boolean ENABLE_TESTS = true;

    private static final List<TestSuite> testSuites = new ArrayList<>();

    public static void register(TestSuite suite) {
        if (!ENABLE_TESTS) return;
        testSuites.add(suite);
    }

    public static List<TestSuite> getTestSuites() {
        return Collections.unmodifiableList(testSuites);
    }

    public static List<TestSuite> getTestSuites(String modId) {
        List<TestSuite> filtered = new ArrayList<>();
        for (TestSuite suite : testSuites) {
            if (suite.getCategory().getModId().equals(modId)) {
                filtered.add(suite);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    public static List<String> getAvailableCategories(boolean isSlave) {
        List<String> categories = new ArrayList<>();
        for (TestSuite suite : testSuites) {
            TestCategory category = suite.getCategory();
            if (category.canRunOn(isSlave)) {
                String name = category.getName();
                if (!categories.contains(name)) {
                    categories.add(name);
                }
            }
        }
        return categories;
    }

    public static List<String> getAvailableCategories(boolean isSlave, String modId) {
        List<String> categories = new ArrayList<>();
        for (TestSuite suite : testSuites) {
            TestCategory category = suite.getCategory();
            if (category.getModId().equals(modId) && category.canRunOn(isSlave)) {
                String name = category.getName();
                if (!categories.contains(name)) {
                    categories.add(name);
                }
            }
        }
        return categories;
    }

    public static void clear() {
        testSuites.clear();
    }
}
