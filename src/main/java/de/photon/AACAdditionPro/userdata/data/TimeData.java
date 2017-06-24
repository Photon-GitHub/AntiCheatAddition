package de.photon.AACAdditionPro.userdata.data;

import de.photon.AACAdditionPro.userdata.Data;
import de.photon.AACAdditionPro.userdata.User;

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
     * @return true if there was an update in the range and false if not.
     */
    public boolean recentlyUpdated(final int index, final long milliseconds)
    {
        return timeStamps[index] + milliseconds >= System.currentTimeMillis();
    }

    // Updating
    public void updateTimeStamp()
    {
        updateTimeStamp(0);
    }

    public void updateTimeStamp(final int index)
    {
        timeStamps[index] = System.currentTimeMillis();
    }

    // Nullifying
    public void nullifyTimeStamp()
    {
        nullifyTimeStamp(0);
    }

    public void nullifyTimeStamp(final int index)
    {
        timeStamps[index] = 0;
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
