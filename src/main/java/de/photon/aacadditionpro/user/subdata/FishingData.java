package de.photon.aacadditionpro.user.subdata;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.user.TimestampKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.datastructures.DoubleStatistics;
import lombok.Getter;

public class FishingData extends SubData
{
    private static final int USED_FISHING_ATTEMPTS = AACAdditionPro.getInstance().getConfig().getInt(ModuleType.AUTO_FISH.getConfigString() + ".parts.consistency.used_fishing_attempts");

    @Getter
    private final DoubleStatistics statistics = new DoubleStatistics();

    /**
     * This represents the amount of fails between two successful fishing times.
     * If this is too high an explanation attempt of an afk fish farm is more sensible than a bot.
     */
    public int failedCounter = 0;

    public FishingData(User user)
    {
        super(user);
    }

    /**
     * Adds a new delta to the consistencyBuffer by reading the last timestamp and buffering the difference between
     * it and {@link System}.currentTimeMillis().
     *
     * @return true if the amount of elements needed for a check is reached.
     */
    public boolean bufferConsistencyData()
    {
        this.statistics.accept((double) this.user.getTimestampMap().passedTime(TimestampKey.AUTOFISH_DETECTION));
        return this.statistics.getCount() >= USED_FISHING_ATTEMPTS;
    }
}
