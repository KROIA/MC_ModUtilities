package net.kroia.modutilities;

import net.kroia.modutilities.persistence.ServerSaveable;
import net.minecraft.nbt.CompoundTag;

/**
 * A simple millisecond-resolution timer with optional auto-restart and persistence support.
 * <p>
 * Times are read from {@link System#currentTimeMillis()} adjusted by {@link #TIMER_OFFSET_MS},
 * making it possible to fast-forward or rewind every timer in tests by changing the offset.
 * <p>
 * The timer implements {@link ServerSaveable}, so its state (start time, duration, auto-restart
 * flag) can be serialized to and restored from a {@link CompoundTag}.
 */
public class TimerMillis implements ServerSaveable {
    /**
     * Global offset added to {@link System#currentTimeMillis()} when reading the current time.
     * Adjust to advance or rewind every {@link TimerMillis} instance simultaneously.
     */
    public static volatile long TIMER_OFFSET_MS = 0;

    private long startTime;
    private long duration;
    private boolean autoRestart;
    private boolean isRunning;

    /**
     * Creates a new timer that is initially stopped.
     *
     * @param autoRestart whether {@link #check()} should automatically restart the timer when it expires
     */
    public TimerMillis(boolean autoRestart) {
        this.startTime = 0;
        this.autoRestart = autoRestart;
        this.duration = 0;
        this.isRunning = false;
    }

    /**
     * Starts (or restarts) the timer with the given duration.
     *
     * @param duration the duration in milliseconds before the timer is considered finished
     */
    public void start(long duration) {
        this.startTime = currentTimeMillis();
        this.duration = duration;
        this.isRunning = true;
    }

    /**
     * Whether the timer has been started and has not yet been stopped or finished.
     *
     * @return {@code true} if the timer is currently running
     */
    public boolean isRunning() {
        return isRunning;
    }
    /**
     * Checks whether the timer has reached its configured duration.
     * <p>
     * As a side effect, transitions the timer to the not-running state when the duration is
     * exceeded.
     *
     * @return {@code true} if the timer has finished, {@code false} otherwise
     */
    public boolean isFinished() {
        if(startTime > 0 && (currentTimeMillis() - startTime) > duration)
        {
            isRunning = false; // Mark as not running if duration exceeded
            return true; // Timer has finished
        }
        return false; // Timer is still running
    }
    /**
     * Stops the timer and resets its start time to zero.
     */
    public void stop() {
        this.isRunning = false;
        this.startTime = 0;
    }
    /**
     * Convenience method that returns {@code true} when the timer has just finished, optionally
     * restarting it if {@link #isAutoRestart()} is enabled.
     *
     * @return {@code true} if the timer has just finished this call, {@code false} otherwise
     */
    public boolean check() {
        if (!isFinished()) {
            return false;
        }
        if (autoRestart) {
            start(duration);
        }
        return true;
    }
    /**
     * Returns how much time has passed since the timer was last started.
     *
     * @return the elapsed time in milliseconds, or {@code 0} if the timer has never been started
     */
    public long getElapsedTime() {
        if (startTime == 0) {
            return 0;
        }
        return currentTimeMillis() - startTime;
    }
    /**
     * Returns how much time remains until the timer reaches its configured duration.
     *
     * @return the remaining time in milliseconds, never negative;
     *         {@code 0} if the timer has finished or has not been started
     */
    public long getRemainingTime() {
        if (startTime == 0) {
            return 0;
        }
        long elapsed = currentTimeMillis() - startTime;
        return Math.max(0, duration - elapsed);
    }
    /**
     * Returns the absolute start time of the most recent {@link #start(long)} call.
     *
     * @return the start time in milliseconds, or {@code 0} if the timer has not been started
     */
    public long getStartTime() {
        return startTime;
    }
    /**
     * Returns the configured duration of the timer.
     *
     * @return the duration in milliseconds
     */
    public long getDuration() {
        return duration;
    }
    /**
     * Updates the timer's duration without changing its start time.
     *
     * @param duration the new duration in milliseconds
     */
    public void setDuration(long duration) {
        this.duration = duration;
    }
    /**
     * Enables or disables auto-restart behaviour for {@link #check()}.
     *
     * @param autoRestart {@code true} to automatically restart the timer when it finishes
     */
    public void setAutoRestart(boolean autoRestart) {
        this.autoRestart = autoRestart;
    }
    /**
     * Whether {@link #check()} will automatically restart this timer once it finishes.
     *
     * @return {@code true} if auto-restart is enabled
     */
    public boolean isAutoRestart() {
        return autoRestart;
    }

    /**
     * Returns the current wall-clock time in milliseconds, adjusted by {@link #TIMER_OFFSET_MS}.
     *
     * @return the current time used by this timer
     */
    public long currentTimeMillis()
    {
        return TIMER_OFFSET_MS + System.currentTimeMillis();
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
        tag.putBoolean("isRunning", isRunning);
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
        // Backward compat: derive from startTime if key is missing (older saves)
        if (tag.contains("isRunning")) {
            this.isRunning = tag.getBoolean("isRunning");
        } else {
            this.isRunning = this.startTime > 0;
        }
        return true;
    }
}
