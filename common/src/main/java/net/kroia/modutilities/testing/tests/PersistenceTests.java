package net.kroia.modutilities.testing.tests;

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

public class PersistenceTests extends TestSuite {

    @Override
    public TestCategory getCategory() {
        return ModUtilitiesTestCategories.PERSISTENCE;
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
                CompoundTag tag = loaded.getCompound(i);
                int idx = tag.getInt("index");
                if (idx != i) {
                    return fail("Element " + i + " has wrong index: " + idx);
                }
                String label = tag.getString("label");
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
