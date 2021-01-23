package de.photon.aacadditionproold.modules.checks.autofish;

import de.photon.aacadditionproold.modules.ListenerModule;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.user.TimestampKey;
import de.photon.aacadditionproold.user.User;
import de.photon.aacadditionproold.user.UserManager;
import de.photon.aacadditionproold.util.datastructures.DoubleStatistics;
import de.photon.aacadditionproold.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionproold.util.files.configs.StringUtil;
import de.photon.aacadditionproold.util.mathematics.MathUtils;
import de.photon.aacadditionproold.util.messaging.VerboseSender;
import lombok.Getter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerFishEvent;

class ConsistencyPattern implements ListenerModule
{
    @Getter
    private static final ConsistencyPattern instance = new ConsistencyPattern();

    @LoadFromConfiguration(configPath = ".min_variation")
    private int minVariation;

    @LoadFromConfiguration(configPath = ".maximum_fails")
    private int maximumFails;

    @EventHandler(ignoreCancelled = true)
    public void onPlayerFish(final PlayerFishEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // User valid and not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

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

                    if (minVariation > maxOffset) {
                        // (maxOffset / minVariation) will be at most 1 and at least 0
                        final double flagOffset = 15 - (14 * (maxOffset / minVariation));

                        VerboseSender.getInstance().sendVerboseMessage("AutoFish-Verbose | Player " +
                                                                       user.getPlayer().getName() +
                                                                       " failed consistency | average time: " +
                                                                       StringUtil.limitStringLength(String.valueOf(consistencyStatistics.getAverage()), 7) +
                                                                       " | maximum offset: " +
                                                                       StringUtil.limitStringLength(String.valueOf(maxOffset), 7) +
                                                                       " | flag offset: " +
                                                                       StringUtil.limitStringLength(String.valueOf(flagOffset), 7));

                        AutoFish.getInstance().getViolationLevelManagement().flag(event.getPlayer(),
                                                                                  (int) flagOffset,
                                                                                  AutoFish.getInstance().getCancelVl(),
                                                                                  () -> event.setCancelled(true), () -> {});
                    }

                    // Reset the statistics.
                    user.getFishingData().getStatistics().reset();
                }

                // Reset the fail counter as just now there was a fishing success.
                user.getFishingData().failedCounter = 0;
                break;
            // No consistency when not fishing / failed fishing
            case IN_GROUND:
            case FAILED_ATTEMPT:
                user.getTimestampMap().nullifyTimeStamp(TimestampKey.AUTOFISH_DETECTION);
                ++user.getFishingData().failedCounter;
                break;
            case CAUGHT_FISH:
                // CAUGHT_FISH covers all forms of items from the water.
                user.getTimestampMap().updateTimeStamp(TimestampKey.AUTOFISH_DETECTION);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean isSubModule()
    {
        return true;
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
