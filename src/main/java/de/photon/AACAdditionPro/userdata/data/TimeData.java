package de.photon.AACAdditionPro.userdata.data;

import de.photon.AACAdditionPro.userdata.Data;
import de.photon.AACAdditionPro.userdata.User;

import java.util.UUID;

public class TimeData extends Data
{
    private final long[] timeStamps;

    public TimeData(final boolean enableListener, final User theUser)
    {
        super(enableListener, theUser);
        timeStamps = new long[]{0};
    }

    public TimeData(final boolean enableListener, final User theUser, final long... timeStamps)
    {
        super(enableListener, theUser);
        this.timeStamps = timeStamps;
    }

    /**
     * Used to test if the timestamp was updated recently
     *
     * @param milliseconds the time-range in which the update should have taken place
     *
     * @return true if there was an update in the range and false if not.
     */
    public boolean recentlyUpdated(final long milliseconds)
    {
        return recentlyUpdated(0, milliseconds);
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
        return timeStamps[index] + milliseconds >= System.currentTimeMillis();
    }

    // Updating

    /**
     * Sets the first (index 0) timestamp to {@link System#currentTimeMillis()}.
     */
    public void updateTimeStamp()
    {
        updateTimeStamp(0);
    }


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
     * Sets a timestamp to {@link System#currentTimeMillis()} if the provided {@link UUID} matches this {@link Data}'s {@link User}'s {@link UUID}.
     *
     * @param uuid the {@link UUID} which the {@link User} should refer to to cause updating.
     */
    public void updateIfRefersToUser(final UUID uuid)
    {
        if (theUser.refersToUUID(uuid))
        {
            this.updateTimeStamp();
        }
    }

    /**
     * Sets a timestamp to {@link System#currentTimeMillis()} if the provided {@link UUID} matches this {@link Data}'s {@link User}'s {@link UUID}.
     *
     * @param uuid  the {@link UUID} which the {@link User} should refer to to cause nullifying.
     * @param index the index of the timestamp.
     */
    public void updateIfRefersToUser(final UUID uuid, final int index)
    {
        if (theUser.refersToUUID(uuid))
        {
            this.nullifyTimeStamp(index);
        }
    }

    // Nullifying

    /**
     * Sets the first (index 0) timestamp to 0.
     */
    public void nullifyTimeStamp()
    {
        nullifyTimeStamp(0);
    }

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
     * Sets the first (index 0) timestamp to 0 if the provided {@link UUID} matches this {@link Data}'s {@link User}'s {@link UUID}.
     *
     * @param uuid the {@link UUID} which the {@link User} should refer to to cause nullifying.
     */
    public void nullifyIfRefersToUser(final UUID uuid)
    {
        if (theUser.refersToUUID(uuid))
        {
            this.nullifyTimeStamp();
        }
    }

    /**
     * Sets a timestamp to 0 if the provided {@link UUID} matches this {@link Data}'s {@link User}'s {@link UUID}.
     *
     * @param uuid  the {@link UUID} which the {@link User} should refer to to cause nullifying.
     * @param index the index of the timestamp.
     */
    public void nullifyIfRefersToUser(final UUID uuid, final int index)
    {
        if (theUser.refersToUUID(uuid))
        {
            this.nullifyTimeStamp(index);
        }
    }

    // Getting
    public long getTimeStamp()
    {
        return getTimeStamp(0);
    }

    public long getTimeStamp(final int index)
    {
        return timeStamps[index];
    }


}
