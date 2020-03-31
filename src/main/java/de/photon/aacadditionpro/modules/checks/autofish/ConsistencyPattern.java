package de.photon.aacadditionpro.modules.checks.autofish;

import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.user.TimestampKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.datastructures.DoubleStatistics;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.general.StringUtils;
import de.photon.aacadditionpro.util.mathematics.MathUtils;
import org.bukkit.event.player.PlayerFishEvent;

class ConsistencyPattern extends PatternModule.Pattern<User, PlayerFishEvent>
{
    @LoadFromConfiguration(configPath = ".violation_offset")
    private int violationOffset;

    @LoadFromConfiguration(configPath = ".maximum_fails")
    private int maximumFails;

    @Override
    protected int process(User user, PlayerFishEvent event)
    {
        switch (event.getState()) {
            case FISHING:
                // Not too many failed attempts in between (afk fish farm false positives)
                // Negative maximum_fails indicate not allowing afk fishing farms.
                if ((maximumFails < 0 || user.getFishingData().failedCounter <= maximumFails) &&
                    // If the last attempt was a fail do not check (false positives)
                    user.getTimestampMap().getTimeStamp(TimestampKey.AUTOFISH_DETECTION) != 0 &&
                    // Add the delta to the consistencyBuffer of the user.
                    user.getFishingData().bufferConsistencyData())
                {
                    // Enough data, now checking
                    final DoubleStatistics consistencyStatistics = user.getFishingData().getStatistics();

                    // Calculate the maximum offset.
                    final double maxOffset = Math.max(MathUtils.offset(consistencyStatistics.getMin(), consistencyStatistics.getAverage()), MathUtils.offset(consistencyStatistics.getMax(), consistencyStatistics.getAverage()));

                    // Ceil in order to make sure that the result is at least 1
                    final double flagOffset = Math.ceil((violationOffset - maxOffset) * 0.5D);

                    message = "AutoFish-Verbose | Player " +
                              user.getPlayer().getName() +
                              " failed consistency | average time: " +
                              StringUtils.limitStringLength(String.valueOf(consistencyStatistics.getAverage()), 7) +
                              " | maximum offset: " +
                              StringUtils.limitStringLength(String.valueOf(maxOffset), 7) +
                              " | flag offset: " +
                              StringUtils.limitStringLength(String.valueOf(flagOffset), 7);

                    user.getFishingData().getStatistics().reset();

                    // Has the player violated the check?
                    return (int) Math.max(flagOffset, 0);
                }

                // Reset the fail counter as just now there was a fishing success.
                user.getFishingData().failedCounter = 0;
                break;
            // No consistency when not fishing / failed fishing
            case IN_GROUND:
            case FAILED_ATTEMPT:
                user.getTimestampMap().nullifyTimeStamp(TimestampKey.AUTOFISH_DETECTION);
                user.getFishingData().failedCounter++;
                break;
            case CAUGHT_FISH:
                // CAUGHT_FISH covers all forms of items from the water.
                user.getTimestampMap().updateTimeStamp(TimestampKey.AUTOFISH_DETECTION);
                break;
            default:
                break;
        }
        return 0;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.consistency";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.AUTO_FISH;
    }
}
