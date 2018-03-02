package de.photon.AACAdditionPro.user;

import java.util.UUID;

public class TimeData extends Data
{
    private final long[] timeStamps;

    public TimeData(final User user, long... timeStamps)
    {
        super(user);
        this.timeStamps = timeStamps;
    }

    /**
     * Calculates how much time has passed since the last update.
     *
     * @param index the index of the time-value
     *
     * @return the passed time in milliseconds.
     */
    public long passedTime(final int index)
    {
        return System.currentTimeMillis() - timeStamps[index];
    }

    /**
     * Used to test if the timestamp was updated recently
     *
     * @param index        the index of the time-value that should be tested
     * @param milliseconds the time-range in which the update should have taken place
     *
     * @return true if there was an update in the range and false if not.
     */
    public boolean recentlyUpdated(final int index, final long milliseconds)
    {
        return this.passedTime(index) <= milliseconds;
    }

    // -------------------------------------------------- Updating -------------------------------------------------- //

    /**
     * Sets a timestamp to {@link System#currentTimeMillis()}.
     *
     * @param index the index of the timestamp.
     */
    public void updateTimeStamp(final int index)
    {
        timeStamps[index] = System.currentTimeMillis();
    }

    /**
     * Sets a timestamp to {@link System#currentTimeMillis()} if the provided {@link UUID} matches this {@link User}'s {@link User}'s {@link UUID}.
     *
     * @param uuid  the {@link UUID} which the {@link User} should refer to to cause nullifying.
     * @param index the index of the timestamp.
     */
    public void updateIfRefersToUser(final UUID uuid, final int index)
    {
        if (this.getUser().refersToUUID(uuid))
        {
            this.updateTimeStamp(index);
        }
    }

    // ------------------------------------------------- Nullifying ------------------------------------------------- //

    /**
     * Sets a timestamp to 0.
     *
     * @param index the index of the timestamp.
     */
    public void nullifyTimeStamp(final int index)
    {
        timeStamps[index] = 0;
    }

    /**
     * Sets a timestamp to 0 if the provided {@link UUID} matches this {@link User}'s {@link User}'s {@link UUID}.
     *
     * @param uuid  the {@link UUID} which the {@link User} should refer to to cause nullifying.
     * @param index the index of the timestamp.
     */
    public void nullifyIfRefersToUser(final UUID uuid, final int index)
    {
        if (this.getUser().refersToUUID(uuid))
        {
            this.nullifyTimeStamp(index);
        }
    }

    public long getTimeStamp(final int index)
    {
        return timeStamps[index];
    }
}
