package net.kroia.modutilities.testing.tests;

import net.kroia.modutilities.NbtCompat;
import net.kroia.modutilities.TimerMillis;
import net.kroia.modutilities.persistence.ChunkedNBT;
import net.kroia.modutilities.persistence.DataPersistence;
import net.kroia.modutilities.persistence.NBTFileParser;
import net.kroia.modutilities.testing.TestCategory;
import net.kroia.modutilities.testing.TestResult;
import net.kroia.modutilities.testing.TestSuite;
import net.kroia.modutilities.testing.categories.ModUtilitiesTestCategories;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class PersistenceTests extends TestSuite {

    @Override
    public TestCategory getCategory() {
        return ModUtilitiesTestCategories.PERSISTENCE;
    }

    @Override
    public boolean needsGame() {
        return false; // passes offline in gradlew :common:test
    }

    @Override
    public void registerTests() {
        // NBTFileParser tests
        addTest("nbtfileparser_max_nbt_size", this::testNBTFileParserMaxNbtSize);
        addTest("nbtfileparser_max_nbt_size_invalid", this::testNBTFileParserMaxNbtSizeInvalid);

        // Uncompressed size tests
        addTest("uncompressed_size_nonempty", this::testUncompressedSizeNonEmpty);
        addTest("uncompressed_size_empty_vs_nonempty", this::testUncompressedSizeEmptyVsNonEmpty);

        // Regression tests
        addTest("DataPersistence_saveDataCompoundList_manyElements", this::testSaveDataCompoundListManyElements);

        // Crash-safe chunked save
        addTest("DataPersistence_chunkedSave_replacesAndCleansUp", this::testChunkedSaveReplacesAndCleansUp);
        addTest("DataPersistence_chunkedLoad_recoversOldFolder", this::testChunkedLoadRecoversOldFolder);
        addTest("DataPersistence_chunkedLoad_ignoresTempFolder", this::testChunkedLoadIgnoresTempFolder);
        addTest("DataPersistence_chunked_dottedNamesRoundTrip", this::testChunkedDottedNamesRoundTrip);

        // Timer save/load regression tests (Issue #15)
        addTest("timer_save_load_preserves_isRunning", this::testTimerSaveLoadPreservesIsRunning);
        addTest("timer_save_load_stopped_stays_stopped", this::testTimerSaveLoadStoppedStaysStopped);
        addTest("timer_save_load_backward_compat", this::testTimerSaveLoadBackwardCompat);
    }

    // ========================================================================
    // NBTFileParser tests
    // ========================================================================

    private TestResult testNBTFileParserMaxNbtSize() {
        long originalMax = NBTFileParser.getMaxNbtSize();
        try {
            NBTFileParser.setMaxNbtSize(4_194_304L); // 4 MB
            long newMax = NBTFileParser.getMaxNbtSize();
            return assertEquals("Max NBT size should be updated to 4 MB", 4_194_304L, newMax);
        } finally {
            // Restore the original value to avoid side effects on other tests
            NBTFileParser.setMaxNbtSize(originalMax);
        }
    }

    private TestResult testNBTFileParserMaxNbtSizeInvalid() {
        return assertThrows(
                "setMaxNbtSize(0) should throw IllegalArgumentException",
                IllegalArgumentException.class,
                () -> NBTFileParser.setMaxNbtSize(0));
    }

    // ========================================================================
    // Uncompressed size tests (NBTFileParser and ChunkedNBT)
    // ========================================================================

    private TestResult testUncompressedSizeNonEmpty() {
        CompoundTag tag = new CompoundTag();
        tag.putString("key", "value");
        long size = NBTFileParser.getUncompressedSize(tag);
        return assertTrue("Uncompressed size of non-empty tag should be > 0", size > 0);
    }

    private TestResult testUncompressedSizeEmptyVsNonEmpty() {
        CompoundTag emptyTag = new CompoundTag();
        CompoundTag fullTag = new CompoundTag();
        fullTag.putString("name", "test");
        fullTag.putInt("count", 42);
        fullTag.putLong("timestamp", 1000000L);

        long emptySize = ChunkedNBT.getUncompressedSize(emptyTag);
        long fullSize = ChunkedNBT.getUncompressedSize(fullTag);

        return assertTrue("Non-empty tag should be larger than empty tag", fullSize > emptySize);
    }

    // ========================================================================
    // Regression tests
    // ========================================================================

    /**
     * N10 regression: saveDataCompoundList handles large lists correctly.
     * Creates 150 small CompoundTag elements, saves them via chunked persistence,
     * reads them back, and verifies every element is preserved.
     */
    private TestResult testSaveDataCompoundListManyElements() {
        final int elementCount = 150;
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("modutil_test_chunked");
        } catch (IOException e) {
            return fail("Failed to create temp directory: " + e.getMessage());
        }

        try {
            DataPersistence persistence = new DataPersistence(
                    DataPersistence.JsonFormat.COMPACT,
                    DataPersistence.NbtFormat.UNCOMPRESSED,
                    Path.of("")
            );
            persistence.setLevelSavePath(tempDir);
            // Suppress debug/warn output during test
            persistence.debugLogger = null;
            persistence.warnLogger = null;

            // Build a ListTag with many small CompoundTag elements
            ListTag dataList = new ListTag();
            for (int i = 0; i < elementCount; i++) {
                CompoundTag entry = new CompoundTag();
                entry.putInt("index", i);
                entry.putString("label", "item_" + i);
                dataList.add(entry);
            }

            Path filePath = tempDir.resolve("test_list.nbt");
            boolean saved = persistence.saveDataCompoundList(filePath, dataList);
            if (!saved) {
                return fail("saveDataCompoundList returned false");
            }

            // Read back
            ListTag loaded = persistence.readDataCompoundList(filePath);
            if (loaded == null) {
                return fail("readDataCompoundList returned null");
            }
            if (loaded.size() != elementCount) {
                return fail("Expected " + elementCount + " elements, got " + loaded.size());
            }

            // Verify each element
            for (int i = 0; i < elementCount; i++) {
                CompoundTag tag = NbtCompat.getCompound(loaded, i);
                int idx = NbtCompat.getInt(tag, "index");
                if (idx != i) {
                    return fail("Element " + i + " has wrong index: " + idx);
                }
                String label = NbtCompat.getString(tag, "label");
                if (!label.equals("item_" + i)) {
                    return fail("Element " + i + " has wrong label: " + label);
                }
            }

            return pass("All " + elementCount + " elements survived save/load roundtrip");
        } finally {
            // Clean up temp files
            try {
                try (var walk = Files.walk(tempDir)) {
                    walk.sorted(java.util.Comparator.reverseOrder())
                            .forEach(p -> {
                                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                            });
                }
            } catch (IOException ignored) {}
        }
    }

    // ========================================================================
    // Crash-safe chunked save
    // ========================================================================

    private static DataPersistence quietPersistence(Path dir) {
        DataPersistence persistence = new DataPersistence(
                DataPersistence.JsonFormat.COMPACT, DataPersistence.NbtFormat.UNCOMPRESSED, Path.of(""));
        persistence.setLevelSavePath(dir);
        persistence.debugLogger = null;
        persistence.warnLogger = null;
        return persistence;
    }

    private static ListTag markerList(int marker, int count) {
        ListTag list = new ListTag();
        for (int i = 0; i < count; i++) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("marker", marker);
            list.add(entry);
        }
        return list;
    }

    /** -1 if the list is empty or mixed, else the common marker. */
    private static int markerOf(ListTag list) {
        if (list.isEmpty()) return -1;
        int marker = NbtCompat.getInt(NbtCompat.getCompound(list, 0), "marker");
        for (int i = 1; i < list.size(); i++)
            if (NbtCompat.getInt(NbtCompat.getCompound(list, i), "marker") != marker) return -1;
        return marker;
    }

    /** Task #137: dotted names keep their dots ("a.b" is not "a"); only a trailing ".nbt" is dropped. */
    private TestResult testChunkedDottedNamesRoundTrip() {
        Path dir;
        try { dir = Files.createTempDirectory("modutil_test_dotted"); }
        catch (IOException e) { return fail("Failed to create temp directory: " + e.getMessage()); }
        try {
            DataPersistence persistence = quietPersistence(dir);
            if (!persistence.saveDataCompoundList(dir.resolve("a.b"), markerList(1, 2))) return fail("save a.b returned false");
            if (!persistence.saveDataCompoundList(dir.resolve("a.nbt"), markerList(2, 3))) return fail("save a.nbt returned false");
            if (!Files.isDirectory(dir.resolve("a.b")) || !Files.isDirectory(dir.resolve("a")))
                return fail("Expected folders a.b and a");
            if (markerOf(persistence.readDataCompoundList(dir.resolve("a.b"))) != 1) return fail("a.b did not read back its own data");
            if (markerOf(persistence.readDataCompoundList(dir.resolve("a"))) != 2) return fail("a did not read back a.nbt data");

            Path mapDir = dir.resolve("map");
            if (!persistence.saveDataCompoundListMap(mapDir, Map.of("x.y", markerList(5, 1), "x", markerList(6, 1))))
                return fail("map save returned false");
            var map = persistence.readDataCompoundListMap(mapDir);
            if (map.size() != 2 || markerOf(map.get("x.y")) != 5 || markerOf(map.get("x")) != 6)
                return fail("Map should round-trip keys x.y and x, got " + map.keySet());
            return pass("Dotted names round-trip");
        } finally {
            deleteTree(dir);
        }
    }

    private static void deleteTree(Path dir) {
        try (var walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        } catch (IOException ignored) {}
    }

    /** A second save replaces the data and leaves no list.tmp / list.old folders or chunk .tmp files. */
    private TestResult testChunkedSaveReplacesAndCleansUp() {
        Path dir;
        try { dir = Files.createTempDirectory("modutil_test_swap"); }
        catch (IOException e) { return fail("Failed to create temp directory: " + e.getMessage()); }
        try {
            DataPersistence persistence = quietPersistence(dir);
            Path file = dir.resolve("list.nbt");
            if (!persistence.saveDataCompoundList(file, markerList(1, 20))) return fail("first save returned false");
            if (!persistence.saveDataCompoundList(file, markerList(2, 5))) return fail("second save returned false");

            ListTag loaded = persistence.readDataCompoundList(file);
            if (loaded.size() != 5 || markerOf(loaded) != 2)
                return fail("Expected 5 entries with marker 2, got " + loaded.size() + " / marker " + markerOf(loaded));
            if (Files.exists(dir.resolve("list.tmp")) || Files.exists(dir.resolve("list.old")))
                return fail("list.tmp or list.old left behind");
            try (var files = Files.list(dir.resolve("list"))) {
                if (files.anyMatch(p -> !p.getFileName().toString().endsWith(".nbt")))
                    return fail("non-chunk file left in list/");
            }
            return pass("Chunked save replaced the data and cleaned up");
        } catch (IOException e) {
            return fail("I/O error: " + e.getMessage());
        } finally {
            deleteTree(dir);
        }
    }

    /** Crash between "list -> list.old" and "list.tmp -> list": only list.old exists, load must return it. */
    private TestResult testChunkedLoadRecoversOldFolder() {
        Path dir;
        try { dir = Files.createTempDirectory("modutil_test_recover"); }
        catch (IOException e) { return fail("Failed to create temp directory: " + e.getMessage()); }
        try {
            DataPersistence persistence = quietPersistence(dir);
            Path file = dir.resolve("list.nbt");
            Path mapDir = dir.resolve("map");
            if (!persistence.saveDataCompoundList(file, markerList(7, 3))) return fail("save returned false");
            if (!persistence.saveDataCompoundList(mapDir.resolve("entry"), markerList(7, 3))) return fail("map save returned false");
            Files.move(dir.resolve("list"), dir.resolve("list.old"));
            Files.move(mapDir.resolve("entry"), mapDir.resolve("entry.old"));

            ListTag loaded = persistence.readDataCompoundList(file);
            if (loaded.size() != 3 || markerOf(loaded) != 7)
                return fail("Expected old data (3 x marker 7), got " + loaded.size() + " / marker " + markerOf(loaded));
            var map = persistence.readDataCompoundListMap(mapDir);
            if (map.size() != 1 || !map.containsKey("entry") || markerOf(map.get("entry")) != 7)
                return fail("Map load should return old data under key 'entry', got " + map.keySet());

            // The next save restores and then replaces it cleanly
            if (!persistence.saveDataCompoundList(file, markerList(8, 2))) return fail("save after crash returned false");
            if (Files.exists(dir.resolve("list.old"))) return fail("list.old left behind after save");
            return assertEquals("Save after crash writes new data", 8, markerOf(persistence.readDataCompoundList(file)));
        } catch (IOException e) {
            return fail("I/O error: " + e.getMessage());
        } finally {
            deleteTree(dir);
        }
    }

    /** Crash while writing list.tmp: load ignores it (also in a list map) and returns the live data. */
    private TestResult testChunkedLoadIgnoresTempFolder() {
        Path dir;
        try { dir = Files.createTempDirectory("modutil_test_stale"); }
        catch (IOException e) { return fail("Failed to create temp directory: " + e.getMessage()); }
        try {
            DataPersistence persistence = quietPersistence(dir);
            Path file = dir.resolve("list.nbt");
            if (!persistence.saveDataCompoundList(file, markerList(3, 4))) return fail("save returned false");
            if (!persistence.saveDataCompoundList(dir.resolve("other.nbt"), markerList(9, 6))) return fail("save returned false");
            Files.move(dir.resolve("other"), dir.resolve("list.tmp")); // stale half-written save

            ListTag loaded = persistence.readDataCompoundList(file);
            if (loaded.size() != 4 || markerOf(loaded) != 3)
                return fail("Expected live data (4 x marker 3), got " + loaded.size() + " / marker " + markerOf(loaded));
            var map = persistence.readDataCompoundListMap(dir);
            if (map.size() != 1 || markerOf(map.get("list")) != 3)
                return fail("Map load should only see 'list' with live data, got " + map.keySet());

            if (!persistence.saveDataCompoundList(file, markerList(4, 1))) return fail("next save returned false");
            return assertFalse("Next save removes the stale temp folder", Files.exists(dir.resolve("list.tmp")));
        } catch (IOException e) {
            return fail("I/O error: " + e.getMessage());
        } finally {
            deleteTree(dir);
        }
    }

    // ========================================================================
    // Timer save/load regression tests (Issue #15)
    // ========================================================================

    /**
     * Issue #15 regression: a running timer must still be running after save/load.
     */
    private TestResult testTimerSaveLoadPreservesIsRunning() {
        TimerMillis timer = new TimerMillis(false);
        timer.start(10000);
        if (!timer.isRunning()) {
            return fail("Timer should be running after start()");
        }

        CompoundTag tag = new CompoundTag();
        timer.save(tag);

        TimerMillis loaded = new TimerMillis(false);
        loaded.load(tag);

        return assertTrue(
                "Issue #15: isRunning must be true after loading a running timer",
                loaded.isRunning());
    }

    /**
     * Issue #15 regression: a stopped timer must stay stopped after save/load.
     */
    private TestResult testTimerSaveLoadStoppedStaysStopped() {
        TimerMillis timer = new TimerMillis(false);
        // Do NOT start it — should remain stopped.

        CompoundTag tag = new CompoundTag();
        timer.save(tag);

        TimerMillis loaded = new TimerMillis(false);
        loaded.load(tag);

        return assertFalse(
                "Issue #15: isRunning must be false after loading a stopped timer",
                loaded.isRunning());
    }

    /**
     * Issue #15 regression: loading a tag from an older save format (no isRunning key)
     * should not crash, and should derive isRunning from startTime > 0.
     */
    private TestResult testTimerSaveLoadBackwardCompat() {
        // Simulate old save format: only startTime, duration, autoRestart (no isRunning key)
        CompoundTag oldFormatRunning = new CompoundTag();
        oldFormatRunning.putLong("startTime", System.currentTimeMillis());
        oldFormatRunning.putLong("duration", 5000L);
        oldFormatRunning.putBoolean("autoRestart", false);

        TimerMillis loadedRunning = new TimerMillis(false);
        boolean loadOk1 = loadedRunning.load(oldFormatRunning);
        if (!loadOk1) {
            return fail("load() should succeed for old format tag with valid keys");
        }
        if (!loadedRunning.isRunning()) {
            return fail("Issue #15 backward compat: isRunning should be true when startTime > 0 and no isRunning key");
        }

        CompoundTag oldFormatStopped = new CompoundTag();
        oldFormatStopped.putLong("startTime", 0L);
        oldFormatStopped.putLong("duration", 0L);
        oldFormatStopped.putBoolean("autoRestart", false);

        TimerMillis loadedStopped = new TimerMillis(false);
        boolean loadOk2 = loadedStopped.load(oldFormatStopped);
        if (!loadOk2) {
            return fail("load() should succeed for old format tag with startTime=0");
        }

        return assertFalse(
                "Issue #15 backward compat: isRunning should be false when startTime == 0 and no isRunning key",
                loadedStopped.isRunning());
    }
}
