package net.kroia.modutilities.persistence.archive;

import net.kroia.modutilities.persistence.NBTFileParser;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for time-bounded NBT data chunks managed by
 * {@link DataArchiveManager}. Each chunk owns a {@link TimeInterval} and is
 * persisted to a single NBT file whose name encodes the start time and
 * duration.
 *
 * @apiNote
 * Subclasses implement {@link #save(CompoundTag)} and {@link #load(CompoundTag)}
 * to serialize their domain data; the framework wraps the data with metadata
 * (start/end times) via {@link #saveInternal(CompoundTag)} and
 * {@link #loadInternal(CompoundTag)}.
 */
public abstract class DataArchiveChunk {

    /**
     * A time range with an inclusive start and a (possibly open-ended) end.
     * An end of {@code -1} is treated as "still open" and resolved to the
     * current wall-clock time on read via {@link #getEndTime()}.
     */
    public static class TimeInterval
    {
        private long startTime;
        private long endTime;

        /**
         * Creates a new time interval.
         *
         * @param startTime the start time in milliseconds since the epoch.
         * @param endTime   the end time in milliseconds since the epoch, or
         *                  {@code -1} to leave the interval open-ended.
         */
        public TimeInterval(long startTime, long endTime)
        {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        /**
         * @return the interval's start time in milliseconds since the epoch.
         */
        public long getStartTime()
        {
            return startTime;
        }
        /**
         * Returns the interval's end time. If the stored end time is the
         * sentinel {@code -1} (open-ended), it is lazily resolved to the
         * current wall-clock time and cached.
         *
         * @return the resolved end time in milliseconds since the epoch.
         */
        public long getEndTime()
        {
            if(endTime == -1)
            {
                endTime = System.currentTimeMillis();
            }
            return endTime;
        }
        /**
         * Updates the interval's end time.
         *
         * @param endTime the new end time in milliseconds since the epoch.
         */
        public void setEndTime(long endTime)
        {
            this.endTime = endTime;
        }
        /**
         * @return the interval's duration in milliseconds.
         */
        public long getDuration()
        {
            return getEndTime() - startTime;
        }

        /**
         * Tests whether this interval ends strictly before the given time.
         *
         * @param time the reference time in milliseconds since the epoch.
         *
         * @return {@code true} if the resolved end time is less than {@code time}.
         */
        public boolean isOlderThan(long time)
        {
            return getEndTime() < time;
        }
        /**
         * Tests whether this interval starts strictly after the given time.
         *
         * @param time the reference time in milliseconds since the epoch.
         *
         * @return {@code true} if the start time is greater than {@code time}.
         */
        public boolean isNewerThan(long time)
        {
            return startTime > time;
        }
        /**
         * Tests whether the given time lies within this interval (inclusive).
         *
         * @param time the time point in milliseconds since the epoch.
         *
         * @return {@code true} if {@code startTime <= time <= endTime}.
         */
        public boolean isInInterval(long time)
        {
            return time >= startTime && time <= getEndTime();
        }
        /**
         * Tests whether this interval overlaps with another.
         *
         * @param other the other interval.
         *
         * @apiNote
         * Uses {@link #getEndTime()} so open-ended intervals (end = {@code -1})
         * correctly report overlap against later intervals.
         *
         * @return {@code true} if the two intervals share at least one instant.
         */
        public boolean overlapsWith(TimeInterval other)
        {
            return (this.startTime < other.getEndTime() && this.getEndTime() > other.getStartTime());
        }
        /**
         * Builds the canonical file name component for this interval in the
         * form {@code "startTime_duration"}.
         *
         * @return the file name component (without extension).
         */
        public String createFileName()
        {
            return startTime+"_"+getDuration();
        }
        /**
         * Parses a {@link TimeInterval} from a file name with the canonical
         * format {@code "startTime_duration[.ext]"}.
         *
         * @param fileName the file name to parse.
         *
         * @return the parsed interval, or {@code null} if the name does not match.
         */
        public static @Nullable TimeInterval fromFileName(String fileName)
        {
            int idx = fileName.lastIndexOf('.');
            if(idx != -1)
            {
                fileName = fileName.substring(0, idx);
            }
            String[] parts = fileName.split("_");
            if(parts.length != 2)
            {
                return null;
            }
            try
            {
                long startTime = Long.parseLong(parts[0]);
                long duration = Long.parseLong(parts[1]);
                return new TimeInterval(startTime, startTime + duration);
            }
            catch (NumberFormatException e)
            {
                return null;
            }
        }
    }

    private TimeInterval timeInterval;


    /**
     * Creates a new chunk whose time interval starts at the current
     * wall-clock time.
     */
    public DataArchiveChunk()
    {
        this(System.currentTimeMillis());
    }
    /**
     * Creates a new chunk with the given start time. The end time is
     * initialized to the same value and is expected to be updated as data is
     * appended.
     *
     * @param startTime the chunk's start time in milliseconds since the epoch.
     */
    public DataArchiveChunk(long startTime)
    {
        timeInterval = new TimeInterval(startTime, startTime);
    }
    /**
     * Creates a new chunk wrapping the given time interval.
     *
     * @param timeInterval the time interval covered by this chunk.
     */
    public DataArchiveChunk(TimeInterval timeInterval)
    {
        this.timeInterval = timeInterval;
    }

    /**
     * @return this chunk's start time in milliseconds since the epoch.
     */
    public long getStartTime()
    {
        return timeInterval.getStartTime();
    }
    /**
     * @return this chunk's end time in milliseconds since the epoch.
     */
    public long getEndTime()
    {
        return timeInterval.getEndTime();
    }
    /**
     * Sets the end time of this chunk's interval to the current wall-clock
     * time.
     *
     * @return the new end time in milliseconds since the epoch.
     */
    public long updateEndTime()
    {
        long time = System.currentTimeMillis();
        timeInterval.setEndTime(time);
        return time;
    }
    /**
     * @return the time interval covered by this chunk.
     */
    public TimeInterval getTimeInterval()
    {
        return timeInterval;
    }
    /**
     * Computes the uncompressed serialized size of this chunk's data tag.
     *
     * @apiNote
     * Internally calls {@link #save(CompoundTag)}; if save fails, returns
     * {@code -1}.
     *
     * @return the uncompressed size in bytes, or {@code -1} on save failure.
     */
    public long getUncompressedSize()
    {
        CompoundTag dataTag = new CompoundTag();
        if(!save(dataTag))
        {
            return -1;
        }
        return NBTFileParser.getUncompressedSize(dataTag);
    }

    /**
     * Serializes this chunk into the given tag, including framework metadata
     * (start/end times under {@code "metadata"}) and the subclass payload
     * (under {@code "data"}).
     *
     * @param tag the tag to populate.
     *
     * @return {@code true} if the subclass {@link #save(CompoundTag)} succeeded.
     */
    public boolean saveInternal(CompoundTag tag)
    {
        CompoundTag dataTag = new CompoundTag();
        if(!save(dataTag))
        {
            return false;
        }
        CompoundTag medadata = new CompoundTag();
        medadata.putLong("startTime", timeInterval.getStartTime());
        medadata.putLong("endTime", timeInterval.getEndTime());
        tag.put("data", dataTag);
        tag.put("metadata", medadata);
        return true;
    }
    /**
     * Restores this chunk from the given tag, reading framework metadata
     * (start/end times) and delegating the payload to the subclass's
     * {@link #load(CompoundTag)}.
     *
     * @param tag the tag containing previously saved chunk data.
     *
     * @apiNote
     * The subclass {@link #load(CompoundTag)} is invoked exactly once.
     *
     * @return {@code true} if the subclass load succeeded.
     */
    public boolean loadInternal(CompoundTag tag)
    {
        CompoundTag metadata = tag.getCompound("metadata");
        this.timeInterval = new TimeInterval(metadata.getLong("startTime"), metadata.getLong("endTime"));
        CompoundTag dataTag = tag.getCompound("data");
        return load(dataTag);
    }

    protected abstract boolean save(CompoundTag dataTag);
    protected abstract boolean load(CompoundTag dataTag);
}
