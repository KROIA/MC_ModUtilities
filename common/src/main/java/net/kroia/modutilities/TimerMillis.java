package net.kroia.modutilities;

import net.kroia.modutilities.persistence.ServerSaveable;
import net.minecraft.nbt.CompoundTag;

public class TimerMillis implements ServerSaveable {
    private long startTime;
    private long duration;
    private boolean autoRestart;
    private boolean isRunning;

    public TimerMillis(boolean autoRestart) {
        this.startTime = 0;
        this.autoRestart = autoRestart;
        this.duration = 0;
        this.isRunning = false;
    }

    public void start(long duration) {
        this.startTime = System.currentTimeMillis();
        this.duration = duration;
        this.isRunning = true;
    }

    public boolean isRunning() {
        return isRunning;
    }
    public boolean isFinished() {
        if(startTime > 0 && (System.currentTimeMillis() - startTime) > duration)
        {
            isRunning = false; // Mark as not running if duration exceeded
            return true; // Timer has finished
        }
        return false; // Timer is still running
    }
    public void stop() {
        this.isRunning = false;
        this.startTime = 0;
    }
    public boolean check() {
        if (!isFinished()) {
            return false;
        }
        if (autoRestart) {
            start(duration);
        }
        return true;
    }
    public long getElapsedTime() {
        if (startTime == 0) {
            return 0;
        }
        return System.currentTimeMillis() - startTime;
    }
    public long getRemainingTime() {
        if (startTime == 0) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.max(0, duration - elapsed);
    }
    public long getStartTime() {
        return startTime;
    }
    public long getDuration() {
        return duration;
    }
    public void setDuration(long duration) {
        this.duration = duration;
    }
    public void setAutoRestart(boolean autoRestart) {
        this.autoRestart = autoRestart;
    }
    public boolean isAutoRestart() {
        return autoRestart;
    }
    @Override
    public String toString() {
        return "TimerMillis{" +
                "startTime=" + startTime +
                ", duration=" + duration +
                ", autoRestart=" + autoRestart +
                '}';
    }

    @Override
    public boolean save(CompoundTag tag) {
        tag.putLong("startTime", startTime);
        tag.putLong("duration", duration);
        tag.putBoolean("autoRestart", autoRestart);
        return true;
    }

    @Override
    public boolean load(CompoundTag tag) {
        if (!tag.contains("startTime") || !tag.contains("duration") || !tag.contains("autoRestart")) {
            return false; // Invalid tag
        }
        this.startTime = tag.getLong("startTime");
        this.duration = tag.getLong("duration");
        this.autoRestart = tag.getBoolean("autoRestart");
        return true;
    }
}
