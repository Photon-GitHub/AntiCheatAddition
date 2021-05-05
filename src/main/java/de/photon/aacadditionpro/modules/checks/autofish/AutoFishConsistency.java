package de.photon.aacadditionpro.modules.checks.autofish;

import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.DataKey;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.datastructure.statistics.DoubleStatistics;
import de.photon.aacadditionpro.util.mathematics.MathUtil;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerFishEvent;

public class AutoFishConsistency extends ViolationModule
{
    @LoadFromConfiguration(configPath = ".min_variation")
    private int minVariation;

    @LoadFromConfiguration(configPath = ".maximum_fails")
    private int maximumFails;

    public AutoFishConsistency()
    {
        super("AutoFish.consistency");
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerFish(final PlayerFishEvent event)
    {
        val user = de.photon.aacadditionpro.user.User.getUser(event.getPlayer().getUniqueId());
        if (User.isUserInvalid(user, bypassPermission)) return;

        switch (event.getState()) {
            case FISHING:
                // Not too many failed attempts in between (afk fish farm false positives)
                // Negative maximum_fails indicate not allowing afk fishing farms.
                if (user.getDataMap().getCounter(DataKey.CounterKey.AUTOFISH_FAILED).compareThreshold() &&
                    // If the last attempt was a fail do not check (false positives)
                    user.getTimestampMap().at(TimestampKey.AUTOFISH_DETECTION).getTime() != 0)
                {
                    // Buffer the data.
                    val consistencyData = (DoubleStatistics) user.getDataMap().getObject(DataKey.ObjectKey.AUTOFISH_CONSISTENCY_DATA);
                    consistencyData.accept(user.getTimestampMap().at(TimestampKey.AUTOFISH_DETECTION).passedTime());


                    // Enough data, now checking
                    final DoubleStatistics consistencyStatistics = user.getFishingData().getStatistics();

                    // Calculate the maximum offset.
                    final double maxOffset = Math.max(MathUtil.absDiff(consistencyStatistics.getMin(), consistencyStatistics.getAverage()), MathUtil.absDiff(consistencyStatistics.getMax(), consistencyStatistics.getAverage()));

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
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .addAllowedServerVersions(ServerVersion.NON_188_VERSIONS)
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return new ViolationLevelManagement(this, 600);
    }
}