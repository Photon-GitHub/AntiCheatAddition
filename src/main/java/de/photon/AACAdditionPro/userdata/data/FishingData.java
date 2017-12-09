package de.photon.AACAdditionPro.userdata.data;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.util.storage.management.DoubleBuffer;

public class FishingData extends TimeData
{
    /**
     * This represents the amount of fails between two successful fishing times.
     * If this is too high an explanation attempt of an afk fish farm is more sensible than a bot.
     */
    public int failedCounter = 0;

    public final DoubleBuffer consistencyBuffer = new DoubleBuffer(AACAdditionPro.getInstance().getConfig().getInt(ModuleType.AUTO_FISH.getConfigString() + ".parts.consistency.consistency_events"))
    {
        @Override
        public boolean verifyObject(Double object)
        {
            return true;
        }
    };

    /**
     * Adds a new delta to the consistencyBuffer by reading the last timestamp and buffering the difference between
     * it and {@link System}.currentTimeMillis().
     *
     * @return true if the amount of elements needed for a check is reached.
     */
    public boolean bufferConsistencyData()
    {
        return this.consistencyBuffer.bufferObject((double) (System.currentTimeMillis() - this.getTimeStamp(1)));
    }

    public FishingData(User theUser)
    {
        super(false, theUser, 0, 0);
    }
}
