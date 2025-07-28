package net.kroia.modutilities;

import net.minecraft.nbt.CompoundTag;

public class TimerMillis implements ServerSaveable{
    private long startTime;
    private long duration;
    private boolean autoRestart;

    public TimerMillis(boolean autoRestart) {
        this.startTime = 0;
        this.autoRestart = autoRestart;
        this.duration = duration;
    }

    public void start(long duration) {
        this.startTime = System.currentTimeMillis();
        this.duration = duration;
    }

    public boolean isRunning() {
        return   startTime > 0 &&
                (System.currentTimeMillis() - startTime) < duration;
    }
    public boolean isFinished() {
        return !isRunning();
    }
    public void stop() {
        this.startTime = 0;
    }
    public boolean check() {
        if (isRunning()) {
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
