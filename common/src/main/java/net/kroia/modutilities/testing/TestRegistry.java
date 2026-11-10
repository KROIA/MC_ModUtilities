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

    /**
     * Test-topic selection (tools/autotest test-selection.json, run_matrix --exclude/--only): comma separated keys
     * {@code <modid>}, {@code <modid>/<Category>} or {@code <modid>/<Category>/<SuiteClass>}. Excluded suites are not
     * run ({@link TestRunner#runAll}, {@link OfflineTests}) and are reported as excluded.
     */
    public static final String EXCLUDE_PROPERTY = "modutilities.tests.exclude";
    /** If set, only suites matching one of these keys run; the rest count as excluded. Exclude applies on top. */
    public static final String INCLUDE_PROPERTY = "modutilities.tests.include";

    /** @return {@code <modid>/<Category>/<SuiteClass>}, the key the selection properties match. */
    public static String topicKey(TestSuite suite) {
        return suite.getCategory().getModId() + "/" + suite.getCategory().getName() + "/" + suite.getClass().getSimpleName();
    }

    public static boolean isExcluded(TestSuite suite) {
        return isExcluded(topicKey(suite), System.getProperty(INCLUDE_PROPERTY), System.getProperty(EXCLUDE_PROPERTY));
    }

    /** @param include, exclude comma separated patterns or null; a blank include means "everything". */
    public static boolean isExcluded(String key, String include, String exclude) {
        return (include != null && !include.isBlank() && !matches(key, include)) || (exclude != null && matches(key, exclude));
    }

    /**
     * @return true if one of the comma separated patterns is a '/'-segment-wise prefix of key; a {@code *} segment
     * matches any one segment (a suite in any category: modid, star, SuiteClass). No regex.
     */
    static boolean matches(String key, String patterns) {
        String[] k = key.split("/");
        for (String p : patterns.split(",")) {
            if (p.isBlank()) continue;
            String[] s = p.trim().split("/");
            int i = 0;
            while (i < s.length && i < k.length && (s[i].equals("*") || s[i].equals(k[i]))) i++;
            if (i == s.length) return true;
        }
        return false;
    }
}
