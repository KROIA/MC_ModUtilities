package net.kroia.modutilities.testing.tests;

import net.kroia.modutilities.persistence.ChunkedNBT;
import net.kroia.modutilities.persistence.NBTFileParser;
import net.kroia.modutilities.persistence.archive.DataArchiveChunk;
import net.kroia.modutilities.testing.TestCategory;
import net.kroia.modutilities.testing.TestResult;
import net.kroia.modutilities.testing.TestSuite;
import net.kroia.modutilities.testing.categories.ModUtilitiesTestCategories;
import net.minecraft.nbt.CompoundTag;

public class PersistenceTests extends TestSuite {

    @Override
    public TestCategory getCategory() {
        return ModUtilitiesTestCategories.PERSISTENCE;
    }

    @Override
    public void registerTests() {
        // TimeInterval tests
        addTest("timeinterval_start_end", this::testTimeIntervalStartEnd);
        addTest("timeinterval_duration", this::testTimeIntervalDuration);
        addTest("timeinterval_is_older_than", this::testTimeIntervalIsOlderThan);
        addTest("timeinterval_is_newer_than", this::testTimeIntervalIsNewerThan);
        addTest("timeinterval_is_in_interval", this::testTimeIntervalIsInInterval);
        addTest("timeinterval_overlap", this::testTimeIntervalOverlap);
        addTest("timeinterval_no_overlap", this::testTimeIntervalNoOverlap);
        addTest("timeinterval_overlap_open_ended_bug", this::testTimeIntervalOverlapOpenEndedBug);
        addTest("timeinterval_filename_roundtrip", this::testTimeIntervalFileNameRoundtrip);
        addTest("timeinterval_filename_with_extension", this::testTimeIntervalFileNameWithExtension);
        addTest("timeinterval_filename_invalid", this::testTimeIntervalFileNameInvalid);

        // DataArchiveChunk tests
        addTest("archive_chunk_save_load", this::testArchiveChunkSaveLoad);
        addTest("archive_chunk_double_load_bug", this::testArchiveChunkDoubleLoadBug);
        addTest("archive_chunk_save_failed", this::testArchiveChunkSaveFailed);

        // NBTFileParser tests
        addTest("nbtfileparser_max_nbt_size", this::testNBTFileParserMaxNbtSize);
        addTest("nbtfileparser_max_nbt_size_invalid", this::testNBTFileParserMaxNbtSizeInvalid);

        // Uncompressed size tests
        addTest("uncompressed_size_nonempty", this::testUncompressedSizeNonEmpty);
        addTest("uncompressed_size_empty_vs_nonempty", this::testUncompressedSizeEmptyVsNonEmpty);
    }

    // ========================================================================
    // TimeInterval tests
    // ========================================================================

    private TestResult testTimeIntervalStartEnd() {
        DataArchiveChunk.TimeInterval interval = new DataArchiveChunk.TimeInterval(100L, 200L);
        if (interval.getStartTime() != 100L) {
            return fail("Expected startTime=100, got " + interval.getStartTime());
        }
        return assertEquals("endTime should be 200", 200L, interval.getEndTime());
    }

    private TestResult testTimeIntervalDuration() {
        DataArchiveChunk.TimeInterval interval = new DataArchiveChunk.TimeInterval(1000L, 3500L);
        return assertEquals("Duration should be endTime - startTime", 2500L, interval.getDuration());
    }

    private TestResult testTimeIntervalIsOlderThan() {
        DataArchiveChunk.TimeInterval interval = new DataArchiveChunk.TimeInterval(100L, 200L);
        if (!interval.isOlderThan(300L)) {
            return fail("Interval [100,200] should be older than 300");
        }
        return assertFalse("Interval [100,200] should NOT be older than 150", interval.isOlderThan(150L));
    }

    private TestResult testTimeIntervalIsNewerThan() {
        DataArchiveChunk.TimeInterval interval = new DataArchiveChunk.TimeInterval(500L, 700L);
        if (!interval.isNewerThan(400L)) {
            return fail("Interval [500,700] should be newer than 400");
        }
        return assertFalse("Interval [500,700] should NOT be newer than 600", interval.isNewerThan(600L));
    }

    private TestResult testTimeIntervalIsInInterval() {
        DataArchiveChunk.TimeInterval interval = new DataArchiveChunk.TimeInterval(100L, 300L);
        if (!interval.isInInterval(200L)) {
            return fail("200 should be in interval [100,300]");
        }
        if (!interval.isInInterval(100L)) {
            return fail("100 (start) should be in interval [100,300]");
        }
        if (!interval.isInInterval(300L)) {
            return fail("300 (end) should be in interval [100,300]");
        }
        return assertFalse("400 should NOT be in interval [100,300]", interval.isInInterval(400L));
    }

    private TestResult testTimeIntervalOverlap() {
        DataArchiveChunk.TimeInterval a = new DataArchiveChunk.TimeInterval(100L, 300L);
        DataArchiveChunk.TimeInterval b = new DataArchiveChunk.TimeInterval(200L, 400L);
        if (!a.overlapsWith(b)) {
            return fail("[100,300] should overlap with [200,400]");
        }
        return assertTrue("[200,400] should overlap with [100,300]", b.overlapsWith(a));
    }

    private TestResult testTimeIntervalNoOverlap() {
        DataArchiveChunk.TimeInterval a = new DataArchiveChunk.TimeInterval(100L, 200L);
        DataArchiveChunk.TimeInterval b = new DataArchiveChunk.TimeInterval(300L, 400L);
        if (a.overlapsWith(b)) {
            return fail("[100,200] should NOT overlap with [300,400]");
        }
        return assertFalse("[300,400] should NOT overlap with [100,200]", b.overlapsWith(a));
    }

    /**
     * Issue #27: overlapsWith uses raw endTime field instead of getEndTime().
     * When endTime is -1 (open-ended), the raw field value -1 is used in the
     * comparison (this.endTime > other.getStartTime()), which evaluates to false
     * even though the interval is still active and should overlap.
     */
    private TestResult testTimeIntervalOverlapOpenEndedBug() {
        // Create an open-ended interval (endTime = -1) meaning "still active"
        DataArchiveChunk.TimeInterval openEnded = new DataArchiveChunk.TimeInterval(100L, -1L);
        DataArchiveChunk.TimeInterval other = new DataArchiveChunk.TimeInterval(200L, 300L);

        // The bug: overlapsWith checks (this.endTime > other.getStartTime())
        // With endTime=-1, this becomes (-1 > 200) which is false.
        // The correct behavior would use getEndTime() which resolves -1 to System.currentTimeMillis().
        boolean result = openEnded.overlapsWith(other);

        // Document the bug: with endTime=-1, the overlap check incorrectly returns false
        // because it uses the raw field (-1) instead of the getter.
        return assertFalse(
                "Issue #27: overlapsWith uses raw endTime=-1 instead of getEndTime(), " +
                "so open-ended interval incorrectly reports no overlap",
                result
        );
    }

    private TestResult testTimeIntervalFileNameRoundtrip() {
        DataArchiveChunk.TimeInterval original = new DataArchiveChunk.TimeInterval(1000L, 5000L);
        String fileName = original.createFileName();
        DataArchiveChunk.TimeInterval parsed = DataArchiveChunk.TimeInterval.fromFileName(fileName);
        if (parsed == null) {
            return fail("fromFileName returned null for: " + fileName);
        }
        if (parsed.getStartTime() != original.getStartTime()) {
            return fail("Start time mismatch after roundtrip: expected " +
                    original.getStartTime() + ", got " + parsed.getStartTime());
        }
        return assertEquals("End time should survive roundtrip",
                original.getEndTime(), parsed.getEndTime());
    }

    private TestResult testTimeIntervalFileNameWithExtension() {
        // fromFileName should strip a file extension before parsing
        DataArchiveChunk.TimeInterval parsed = DataArchiveChunk.TimeInterval.fromFileName("1000_4000.nbt");
        if (parsed == null) {
            return fail("fromFileName returned null for '1000_4000.nbt'");
        }
        if (parsed.getStartTime() != 1000L) {
            return fail("Expected startTime=1000, got " + parsed.getStartTime());
        }
        // duration=4000, so endTime = 1000 + 4000 = 5000
        return assertEquals("endTime should be startTime + duration", 5000L, parsed.getEndTime());
    }

    private TestResult testTimeIntervalFileNameInvalid() {
        DataArchiveChunk.TimeInterval result1 = DataArchiveChunk.TimeInterval.fromFileName("invalid");
        if (result1 != null) {
            return fail("fromFileName('invalid') should return null");
        }
        DataArchiveChunk.TimeInterval result2 = DataArchiveChunk.TimeInterval.fromFileName("a_b_c");
        if (result2 != null) {
            return fail("fromFileName('a_b_c') should return null (3 parts)");
        }
        DataArchiveChunk.TimeInterval result3 = DataArchiveChunk.TimeInterval.fromFileName("abc_def");
        return assertNull("fromFileName('abc_def') should return null (non-numeric)", result3);
    }

    // ========================================================================
    // DataArchiveChunk tests
    // ========================================================================

    /**
     * A concrete implementation of DataArchiveChunk for testing purposes.
     */
    private static class TestChunk extends DataArchiveChunk {
        private String data = "";
        private int loadCount = 0;

        public TestChunk(long startTime) {
            super(startTime);
        }

        public TestChunk(TimeInterval timeInterval) {
            super(timeInterval);
        }

        public void setData(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }

        public int getLoadCount() {
            return loadCount;
        }

        @Override
        protected boolean save(CompoundTag dataTag) {
            dataTag.putString("testData", data);
            return true;
        }

        @Override
        protected boolean load(CompoundTag dataTag) {
            loadCount++;
            data = dataTag.getString("testData");
            return true;
        }
    }

    private TestResult testArchiveChunkSaveLoad() {
        TestChunk original = new TestChunk(1000L);
        original.getTimeInterval().setEndTime(2000L);
        original.setData("hello world");

        // Save to a CompoundTag
        CompoundTag tag = new CompoundTag();
        boolean saved = original.saveInternal(tag);
        if (!saved) {
            return fail("saveInternal should return true");
        }

        // Load into a new chunk
        TestChunk loaded = new TestChunk(0L);
        boolean loadedOk = loaded.loadInternal(tag);
        if (!loadedOk) {
            return fail("loadInternal should return true");
        }

        if (loaded.getStartTime() != 1000L) {
            return fail("Loaded startTime should be 1000, got " + loaded.getStartTime());
        }
        if (loaded.getEndTime() != 2000L) {
            return fail("Loaded endTime should be 2000, got " + loaded.getEndTime());
        }
        return assertEquals("Loaded data should match", "hello world", loaded.getData());
    }

    /**
     * Issue #8: loadInternal calls load(dataTag) twice.
     * The first call is in the if-condition, and the second call is explicit on line 159.
     * This means the load() method is invoked twice for a single loadInternal() call.
     */
    private TestResult testArchiveChunkDoubleLoadBug() {
        TestChunk original = new TestChunk(1000L);
        original.getTimeInterval().setEndTime(2000L);
        original.setData("test data");

        CompoundTag tag = new CompoundTag();
        original.saveInternal(tag);

        TestChunk loaded = new TestChunk(0L);
        loaded.loadInternal(tag);

        // Issue #8: load() is called twice inside loadInternal()
        // Line 152: if(!load(dataTag)) -- first call
        // Line 159: return this.load(dataTag); -- second call
        return assertEquals(
                "Issue #8: loadInternal calls load() twice - loadCount should be 2",
                2, loaded.getLoadCount());
    }

    /**
     * Tests that saveInternal returns false when save() fails.
     */
    private TestResult testArchiveChunkSaveFailed() {
        DataArchiveChunk failingChunk = new DataArchiveChunk(1000L) {
            @Override
            protected boolean save(CompoundTag dataTag) {
                return false; // Simulate save failure
            }

            @Override
            protected boolean load(CompoundTag dataTag) {
                return true;
            }
        };

        CompoundTag tag = new CompoundTag();
        return assertFalse("saveInternal should return false when save() fails",
                failingChunk.saveInternal(tag));
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
}
