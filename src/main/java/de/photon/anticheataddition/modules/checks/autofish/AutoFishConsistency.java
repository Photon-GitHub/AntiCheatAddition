package de.photon.anticheataddition.modules.checks.autofish;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.DataKey;
import de.photon.anticheataddition.user.data.TimestampKey;
import de.photon.anticheataddition.util.config.LoadFromConfiguration;
import de.photon.anticheataddition.util.datastructure.statistics.DoubleStatistics;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

public class AutoFishConsistency extends ViolationModule implements Listener
{
    private final int cancelVl = AntiCheatAddition.getInstance().getConfig().getInt("AutoFish.cancel_vl");

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
        val user = de.photon.anticheataddition.user.User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        switch (event.getState()) {
            case FISHING:
                // Not too many failed attempts in between (afk fish farm false positives)
                // Negative maximum_fails indicate not allowing afk fishing farms.
                if (user.getDataMap().getCounter(DataKey.Count.AUTOFISH_FAILED).compareThreshold() &&
                    // If the last attempt was a fail do not check (false positives)
                    user.getTimestampMap().at(TimestampKey.AUTOFISH_DETECTION).getTime() != 0)
                {
                    // Buffer the data.
                    val consistencyData = (DoubleStatistics) user.getDataMap().getObject(DataKey.Obj.AUTOFISH_CONSISTENCY_DATA);
                    consistencyData.accept(user.getTimestampMap().at(TimestampKey.AUTOFISH_DETECTION).passedTime());

                    // Check that we have enough data.
                    if (consistencyData.getCount() < fishingAttemptCount) return;

                    // Calculate the maximum offset.
                    val maxOffset = Math.max(MathUtil.absDiff(consistencyData.getMin(), consistencyData.getAverage()), MathUtil.absDiff(consistencyData.getMax(), consistencyData.getAverage()));

                    if (minVariation > (maxOffset + 1)) {
                        // (maxOffset / minVariation) will be at most 1 and at least 0
                        val flagOffset = 160 - (159 * (maxOffset / minVariation));

                        this.getManagement().flag(Flag.of(event.getPlayer())
                                                      .setAddedVl((int) flagOffset)
                                                      .setDebug("AutoFish-Debug | Player " +
                                                                user.getPlayer().getName() +
                                                                " failed consistency | average time: " +
                                                                StringUtils.left(String.valueOf(consistencyData.getAverage()), 7) +
                                                                " | maximum offset: " +
                                                                StringUtils.left(String.valueOf(maxOffset), 7) +
                                                                " | flag offset: " +
                                                                StringUtils.left(String.valueOf(flagOffset), 7))
                                                      .setCancelAction(cancelVl, () -> event.setCancelled(true)));
                    }

                    // Reset the statistics.
                    consistencyData.reset();
                }

                // Reset the fail counter as just now there was a fishing success.
                user.getDataMap().getCounter(DataKey.Count.AUTOFISH_FAILED).setToZero();
                break;
            // No consistency when not fishing / failed fishing
            case IN_GROUND:
            case FAILED_ATTEMPT:
                user.getTimestampMap().at(TimestampKey.AUTOFISH_DETECTION).setToZero();
                user.getDataMap().getCounter(DataKey.Count.AUTOFISH_FAILED).increment();
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
                           .setAllowedServerVersions(ServerVersion.NON_188_VERSIONS)
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(800, 3).build();
    }
}