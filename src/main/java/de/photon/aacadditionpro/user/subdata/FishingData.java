package de.photon.aacadditionpro.user.subdata;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.user.DataKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.datastructures.DoubleStatistics;
import lombok.Getter;

public class FishingData extends SubData
{
    private static final int CONSISTENCY_EVENTS = AACAdditionPro.getInstance().getConfig().getInt(ModuleType.AUTO_FISH.getConfigString() + ".parts.consistency.consistency_events");

    @Getter
    private final DoubleStatistics statistics = new DoubleStatistics();

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
        this.statistics.accept((double) this.user.getDataMap().passedTime(DataKey.AUTOFISH_DETECTION));
        return this.statistics.getCount() >= CONSISTENCY_EVENTS;
    }
}
