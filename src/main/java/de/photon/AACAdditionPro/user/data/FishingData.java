package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.datastructures.DoubleStatistics;
import lombok.Getter;

public class FishingData extends TimeData
{
    private static final int CONSISTENCY_EVENTS = AACAdditionPro.getInstance().getConfig().getInt(ModuleType.AUTO_FISH.getConfigString() + ".parts.consistency.consistency_events");

    /**
     * This represents the amount of fails between two successful fishing times.
     * If this is too high an explanation attempt of an afk fish farm is more sensible than a bot.
     */
    public int failedCounter = 0;

    public boolean lastAttemptSuccessful = false;

    @Getter
    private final DoubleStatistics statistics = new DoubleStatistics();

    public FishingData(final User user)
    {
        // [0] = Timestamp of last fish bite (PlayerFishEvent.State.BITE)
        super(user, 0);
    }

    /**
     * Adds a new delta to the consistencyBuffer by reading the last timestamp and buffering the difference between
     * it and {@link System}.currentTimeMillis().
     *
     * @return true if the amount of elements needed for a check is reached.
     */
    public boolean bufferConsistencyData()
    {
        this.statistics.getSummaryStatistics().accept((double) (System.currentTimeMillis() - this.getTimeStamp(1)));
        return this.statistics.getSummaryStatistics().getCount() >= CONSISTENCY_EVENTS;
    }
}
