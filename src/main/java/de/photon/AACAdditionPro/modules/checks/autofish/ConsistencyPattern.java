package de.photon.AACAdditionPro.modules.checks.autofish;

import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.general.StringUtils;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.DoubleSummaryStatistics;

class ConsistencyPattern extends PatternModule.Pattern<User, PlayerFishEvent>
{
    @LoadFromConfiguration(configPath = ".violation_offset")
    private int violation_offset;

    @LoadFromConfiguration(configPath = ".maximum_fails")
    private int maximum_fails;

    @Override
    protected int process(User user, PlayerFishEvent event)
    {
        switch (event.getState())
        {
            case FISHING:
                // Not too many failed attempts in between (afk fish farm false positives)
                // Negative maximum_fails indicate not allowing afk fishing farms.
                if ((maximum_fails < 0 || user.getFishingData().failedCounter <= maximum_fails) &&
                    // If the last attempt was a fail do not check (false positives)
                    user.getFishingData().lastAttemptSuccessful &&
                    // Add the delta to the consistencyBuffer of the user.
                    user.getFishingData().bufferConsistencyData())
                {
                    // Enough data, now checking
                    final DoubleSummaryStatistics consistencyStatistics = user.getFishingData().getStatistics().getSummaryStatistics();

                    // Calculate the maximum offset.
                    final double maxOffset = Math.max(MathUtils.offset(consistencyStatistics.getMin(), consistencyStatistics.getAverage()), MathUtils.offset(consistencyStatistics.getMax(), consistencyStatistics.getAverage()));

                    // Ceil in order to make sure that the result is at least 1
                    final double flagOffset = Math.ceil((violation_offset - maxOffset) * 0.5D);

                    VerboseSender.getInstance().sendVerboseMessage("AutoFish-Verbose | Player " +
                                                                   user.getPlayer().getName() +
                                                                   " failed consistency | average time: " +
                                                                   StringUtils.limitStringLength(String.valueOf(consistencyStatistics.getAverage()), 7) +
                                                                   " | maximum offset: " +
                                                                   StringUtils.limitStringLength(String.valueOf(maxOffset), 7) +
                                                                   " | flag offset: " +
                                                                   StringUtils.limitStringLength(String.valueOf(flagOffset), 7));

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
                user.getFishingData().lastAttemptSuccessful = false;
                user.getFishingData().failedCounter++;
                break;
            case CAUGHT_FISH:
                // CAUGHT_FISH covers all forms of items from the water.
                user.getFishingData().lastAttemptSuccessful = true;
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
