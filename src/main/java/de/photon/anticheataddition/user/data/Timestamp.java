package de.photon.anticheataddition.user.data;

import java.util.concurrent.TimeUnit;

/**
 * A class to manage a timestamp.
 */
public final class Timestamp
{
    // Only set and get operations -> no atomic required.
    private volatile long currentNanoTime = 0;

    public long getTime()
    {
        return TimeUnit.NANOSECONDS.toMillis(currentNanoTime);
    }

    /**
     * Updates this {@link Timestamp} to the current time as given by {@link System#nanoTime()}.
     */
    public void update()
    {
        this.currentNanoTime = System.nanoTime();
    }

    /**
     * Sets this {@link Timestamp} to 0.
     */
    public void setToZero()
    {
        this.currentNanoTime = 0;
    }

    /**
     * Sets this {@link Timestamp} to a time in the future.
     */
    public void setToFuture(long futureMillis)
    {
        this.currentNanoTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(futureMillis);
    }

    /**
     * This determines and returns the time which has passed since the {@link Timestamp} was updated the last time.
     *
     * @return the passed time in milliseconds.
     */
    public long passedTime()
    {
        return TimeUnit.NANOSECONDS.toMillis(this.passedNanos());
    }

    /**
     * Returns the time passed since the monotonic nanosecond timestamp was updated.
     */
    public long passedNanos()
    {
        return System.nanoTime() - currentNanoTime;
    }

    /**
     * Checks if this {@link Timestamp} has a value that is at most the specified time ago.
     *
     * @param time the time which has passed at most for this method to return true.
     * @return true if the internal time is smaller or equal to the specified time, false otherwise.
     * If the internal time is 0 (via {@link #setToZero()}) this method will return false.
     * If the internal time is in the future (via {@link #setToFuture(long)}) this method will return true.
     */
    public boolean recentlyUpdated(long time)
    {
        return this.passedTime() <= time;
    }

    /**
     * Opposite of {@link #recentlyUpdated(long)}
     */
    public boolean notRecentlyUpdated(long time)
    {
        return this.passedTime() > time;
    }
}
