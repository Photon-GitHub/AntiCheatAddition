package de.photon.aacadditionpro.modules.checks.autofish;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.DataKey;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.datastructure.statistics.DoubleStatistics;
import de.photon.aacadditionpro.util.mathematics.MathUtil;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

public class AutoFishConsistency extends ViolationModule implements Listener
{
    private final int cancelVl = AACAdditionPro.getInstance().getConfig().getInt("AutoFish.cancel_vl");

    @LoadFromConfiguration(configPath = ".min_variation")
    private int minVariation;

    @LoadFromConfiguration(configPath = ".fishing_attempt_count")
    private int fishingAttemptCount;

    public AutoFishConsistency()
    {
        super("AutoFish.parts.consistency");
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

                    // Check that we have enough data.
                    if (consistencyData.getCount() < fishingAttemptCount) return;

                    // Calculate the maximum offset.
                    val maxOffset = Math.max(MathUtil.absDiff(consistencyData.getMin(), consistencyData.getAverage()), MathUtil.absDiff(consistencyData.getMax(), consistencyData.getAverage()));

                    if (minVariation > maxOffset) {
                        // (maxOffset / minVariation) will be at most 1 and at least 0
                        val flagOffset = 15 - (14 * (maxOffset / minVariation));

                        DebugSender.getInstance().sendVerboseMessage("AutoFish-Verbose | Player " +
                                                                     user.getPlayer().getName() +
                                                                     " failed consistency | average time: " +
                                                                     StringUtils.left(String.valueOf(consistencyData.getAverage()), 7) +
                                                                     " | maximum offset: " +
                                                                     StringUtils.left(String.valueOf(maxOffset), 7) +
                                                                     " | flag offset: " +
                                                                     StringUtils.left(String.valueOf(flagOffset), 7));

                        this.getManagement().flag(Flag.of(event.getPlayer())
                                                      .setAddedVl((int) flagOffset)
                                                      .setCancelAction(cancelVl, () -> event.setCancelled(true)));
                    }

                    // Reset the statistics.
                    consistencyData.reset();
                }

                // Reset the fail counter as just now there was a fishing success.
                user.getDataMap().getCounter(DataKey.CounterKey.AUTOFISH_FAILED).setToZero();
                break;
            // No consistency when not fishing / failed fishing
            case IN_GROUND:
            case FAILED_ATTEMPT:
                user.getTimestampMap().at(TimestampKey.AUTOFISH_DETECTION).setToZero();
                user.getDataMap().getCounter(DataKey.CounterKey.AUTOFISH_FAILED).increment();
                break;
            case CAUGHT_FISH:
                // CAUGHT_FISH covers all forms of items from the water.
                user.getTimestampMap().at(TimestampKey.AUTOFISH_DETECTION).update();
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