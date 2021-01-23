package de.photon.aacadditionproold.user;

/**
 * A {@link DataMap} specifically designed to hold times.
 * The map has various methods to update and set timestamps using the internal {@link System#currentTimeMillis()} to
 * get the timestamps.
 */
public class TimestampMap<T extends Enum<T>> extends DataMap<T, Long>
{
    /**
     * Creates a new {@link TimestampMap}
     */
    public TimestampMap(Class<T> enumeration)
    {
        super(enumeration);
    }

    /**
     * Get a timestamp.
     */
    public Long getTimeStamp(T key)
    {
        return this.map.get(key);
    }

    /**
     * This determines and returns the time which has passed since the timestamp was updated the last time.
     * If the timestamp was manually set to a date in the future this method will return a negative value.
     *
     * @return the passed time in milliseconds.
     */
    public Long passedTime(T key)
    {
        return System.currentTimeMillis() - this.map.get(key);
    }

    /**
     * Updates a timestamp to the current time as given by {@link System#currentTimeMillis()}.
     */
    public void updateTimeStamp(T key)
    {
        this.map.put(key, System.currentTimeMillis());
    }

    /**
     * This updates multiple timestamps to the current timestamp and guarantees that all given timestamps have the same
     * timestamp. Using this method when applicable improves performance by only querying the time once.
     */
    public void updateTimeStamps(T... keys)
    {
        long currentTime = System.currentTimeMillis();
        for (T key : keys) {
            this.map.put(key, currentTime);
        }
    }

    /**
     * Sets a timestamp to 0.
     */
    public void nullifyTimeStamp(T key)
    {
        this.map.put(key, 0L);
    }

    /**
     * Sets multiple timestamps to 0.
     */
    public void nullifyTimeStamps(T... keys)
    {
        for (T key : keys) {
            this.map.put(key, 0L);
        }
    }

    /**
     * Checks if a timestamp has a value that is at most the specified time ago.
     * Note that if the timestamp was nullified before via {@link #nullifyTimeStamp(Enum)} or {@link #nullifyTimeStamps(Enum[])}
     * this method will return false.
     * Also, if the timestamp has been set manually to a date in the future, this method will return true.
     *
     * @param time the time which has passed at most for this method to return true.
     */
    public boolean recentlyUpdated(T key, long time)
    {
        return this.passedTime(key) <= time;
    }
}
