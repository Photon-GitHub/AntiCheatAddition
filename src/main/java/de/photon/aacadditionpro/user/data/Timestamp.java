package de.photon.aacadditionpro.user.data;

/**
 * A class to manage a timestamp.
 */
public class Timestamp
{
    // Only set and get operations -> no atomic required.
    private volatile long currentTime = 0;

    private long getTime()
    {
        return currentTime;
    }

    /**
     * Updates this {@link Timestamp} to the current time as given by {@link System#currentTimeMillis()}.
     */
    private void updateTime()
    {
        this.currentTime = System.currentTimeMillis();
    }

    /**
     * Sets this {@link Timestamp} to 0.
     */
    private void setToZero()
    {
        this.currentTime = 0;
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
     * Note that if the {@link Timestamp} was nullified before via {@link #setToZero()} this method will return false.
     *
     * @param time the time which has passed at most for this method to return true.
     */
    public boolean recentlyUpdated(long time)
    {
        return this.passedTime() <= time;
    }
}
