package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.datastructures.DoubleBuffer;

public class FishingData extends TimeData
{
    /**
     * This represents the amount of fails between two successful fishing times.
     * If this is too high an explanation attempt of an afk fish farm is more sensible than a bot.
     */
    public int failedCounter = 0;

    public final DoubleBuffer consistencyBuffer = new DoubleBuffer(AACAdditionPro.getInstance().getConfig().getInt(ModuleType.AUTO_FISH.getConfigString() + ".parts.consistency.consistency_events"));

    public FishingData(final User user)
    {
        super(user, 0, 0);
    }

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
}
