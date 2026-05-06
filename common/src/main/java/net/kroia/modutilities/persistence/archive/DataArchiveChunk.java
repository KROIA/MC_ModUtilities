package net.kroia.modutilities.persistence.archive;

import net.kroia.modutilities.persistence.NBTFileParser;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public abstract class DataArchiveChunk {

    public static class TimeInterval
    {
        private long startTime;
        private long endTime;

        public TimeInterval(long startTime, long endTime)
        {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public long getStartTime()
        {
            return startTime;
        }
        public long getEndTime()
        {
            if(endTime == -1)
            {
                endTime = System.currentTimeMillis();
            }
            return endTime;
        }
        public void setEndTime(long endTime)
        {
            this.endTime = endTime;
        }
        public long getDuration()
        {
            return getEndTime() - startTime;
        }

        public boolean isOlderThan(long time)
        {
            return getEndTime() < time;
        }
        public boolean isNewerThan(long time)
        {
            return startTime > time;
        }
        public boolean isInInterval(long time)
        {
            return time >= startTime && time <= getEndTime();
        }
        public boolean overlapsWith(TimeInterval other)
        {
            return (this.startTime < other.getEndTime() && this.getEndTime() > other.getStartTime());
        }
        public String createFileName()
        {
            return startTime+"_"+getDuration();
        }
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


    public DataArchiveChunk()
    {
        this(System.currentTimeMillis());
    }
    public DataArchiveChunk(long startTime)
    {
        timeInterval = new TimeInterval(startTime, startTime);
    }
    public DataArchiveChunk(TimeInterval timeInterval)
    {
        this.timeInterval = timeInterval;
    }

    public long getStartTime()
    {
        return timeInterval.getStartTime();
    }
    public long getEndTime()
    {
        long time = timeInterval.getEndTime();
        if(time == -1)
        {
            time = System.currentTimeMillis();
            timeInterval.setEndTime(time);
        }
        return time;
    }
    public long updateEndTime()
    {
        long time = System.currentTimeMillis();
        timeInterval.setEndTime(time);
        return time;
    }
    public TimeInterval getTimeInterval()
    {
        return timeInterval;
    }
    public long getUncompressedSize()
    {
        CompoundTag dataTag = new CompoundTag();
        if(!save(dataTag))
        {
            return -1;
        }
        return NBTFileParser.getUncompressedSize(dataTag);
    }

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
