package de.photon.anticheataddition.modules.checks.autofish;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.DataKey;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.datastructure.statistics.DoubleStatistics;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

public final class AutoFishConsistency extends ViolationModule implements Listener
{
    public static final AutoFishConsistency INSTANCE = new AutoFishConsistency();

    private final int cancelVl = AntiCheatAddition.getInstance().getConfig().getInt("AutoFish.cancel_vl");

    private final int fishingAttemptCount = loadInt(".fishing_attempt_count", 5);
    private final int minVariation = loadInt(".min_variation", 30);

    private AutoFishConsistency()
    {
        super("AutoFish.parts.consistency");
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerFish(final PlayerFishEvent event)
    {
        val user = de.photon.anticheataddition.user.User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        switch (event.getState()) {
            case FISHING -> {
                // Not too many failed attempts in between (afk fish farm false positives)
                // Negative maximum_fails indicate not allowing afk fishing farms.
                if (user.getDataMap().getCounter(DataKey.Count.AUTOFISH_FAILED).compareThreshold() &&
                    // If the last attempt was a fail do not check (false positives)
                    user.getTimeMap().at(TimeKey.AUTOFISH_DETECTION).getTime() != 0)
                {
                    // Buffer the data.
                    val consistencyData = (DoubleStatistics) user.getDataMap().getObject(DataKey.Obj.AUTOFISH_CONSISTENCY_DATA);
                    consistencyData.accept(user.getTimeMap().at(TimeKey.AUTOFISH_DETECTION).passedTime());

                    // Check that we have enough data.
                    if (consistencyData.getCount() < fishingAttemptCount) return;

                    // Calculate the maximum offset.
                    final double maxOffset = Math.max(MathUtil.absDiff(consistencyData.getMin(), consistencyData.getAverage()), MathUtil.absDiff(consistencyData.getMax(), consistencyData.getAverage()));

                    if (minVariation > (maxOffset + 1)) {
                        // (maxOffset / minVariation) will be at most 1 and at least 0
                        final double flagOffset = 160 - (159 * (maxOffset / minVariation));

                        this.getManagement().flag(Flag.of(event.getPlayer())
                                                      .setAddedVl((int) flagOffset)
                                                      .setDebug(() -> "AutoFish-Debug | Player %s failed consistency | average time: %f | maximum offset: %f | flag offset: %f"
                                                              .formatted(user.getPlayer().getName(),
                                                                         consistencyData.getAverage(),
                                                                         maxOffset,
                                                                         flagOffset))
                                                      .setCancelAction(cancelVl, () -> event.setCancelled(true)));
                    }

                    // Reset the statistics.
                    consistencyData.reset();
                }

                // Reset the fail counter as just now there was a fishing success.
                user.getDataMap().getCounter(DataKey.Count.AUTOFISH_FAILED).setToZero();
            }
            // No consistency when not fishing / failed fishing
            case IN_GROUND, FAILED_ATTEMPT -> {
                user.getTimeMap().at(TimeKey.AUTOFISH_DETECTION).setToZero();
                user.getDataMap().getCounter(DataKey.Count.AUTOFISH_FAILED).increment();
            }
            // CAUGHT_FISH covers all forms of items from the water.
            case CAUGHT_FISH -> user.getTimeMap().at(TimeKey.AUTOFISH_DETECTION).update();
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