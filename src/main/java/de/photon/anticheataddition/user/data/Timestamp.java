package de.photon.anticheataddition.user.data;

/**
 * A class to manage a timestamp.
 */
public final class Timestamp
{
    // Only set and get operations -> no atomic required.
    private volatile long currentTime = 0;

    public long getTime()
    {
        return currentTime;
    }

    /**
     * Updates this {@link Timestamp} to the current time as given by {@link System#currentTimeMillis()}.
     */
    public void update()
    {
        this.currentTime = System.currentTimeMillis();
    }

    /**
     * Sets this {@link Timestamp} to 0.
     */
    public void setToZero()
    {
        this.currentTime = 0;
    }

    /**
     * Sets this {@link Timestamp} to a time in the future.
     */
    public void setToFuture(long futureMillis)
    {
        this.currentTime = System.currentTimeMillis() + futureMillis;
    }

    /**
     * This determines and returns the time which has passed since the {@link Timestamp} was updated the last time.
     *
     * @return the passed time in milliseconds.
     */
    public long passedTime()
    {
        return System.currentTimeMillis() - currentTime;
    }

    /**
     * Checks if this {@link Timestamp} has a value that is at most the specified time ago.
     *
     * @param time the time which has passed at most for this method to return true.
     *
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
