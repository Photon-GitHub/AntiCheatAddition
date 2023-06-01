package de.photon.anticheataddition.modules.checks.autofish;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.messaging.Log;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

public final class AutoFishConsistency extends ViolationModule implements Listener
{
    public static final AutoFishConsistency INSTANCE = new AutoFishConsistency();

    private static final int FISHING_ATTEMPTS_TO_CHECK = 5;
    private static final int MIN_HUMAN_VARIATION = 50;

    private final int cancelVl = AntiCheatAddition.getInstance().getConfig().getInt("AutoFish.cancel_vl");

    private final boolean allowAfkFishingFarms = loadBoolean(".allow_afk_fishing_farms", true);

    private AutoFishConsistency()
    {
        super("AutoFish.parts.consistency");
    }

    private void checkConsistency(TimeKey key, User user, PlayerFishEvent event)
    {
        // If the last attempt was a fail do not check (false positives)
        if (user.getTimeMap().at(key).getTime() != 0) {
            // Buffer the data.
            final var consistencyData = user.getData().object.autoFishConsistencyData.get(key);
            final long passedTime = user.getTimeMap().at(key).passedTime();

            consistencyData.accept(passedTime);
            Log.finer(() -> "AutoFish-Debug | Time for key " + key + ": " + passedTime);

            // Check that we have enough data.
            if (consistencyData.getCount() < FISHING_ATTEMPTS_TO_CHECK) return;

            // Calculate the maximum offset.
            final double maxOffset = Math.max(MathUtil.absDiff(consistencyData.getMin(), consistencyData.getAverage()), MathUtil.absDiff(consistencyData.getMax(), consistencyData.getAverage()));

            if (MIN_HUMAN_VARIATION > (maxOffset + 1)) {
                // (maxOffset / minVariation) will be at most 1 and at least 0
                final double flagOffset = 130 - (129 * (maxOffset / MIN_HUMAN_VARIATION));

                this.getManagement().flag(Flag.of(user)
                                              .setAddedVl((int) flagOffset)
                                              .setDebug(() -> "AutoFish-Debug | Player %s failed consistency | key %s | average time: %f | maximum offset: %f | flag offset: %f"
                                                      .formatted(user.getPlayer().getName(),
                                                                 key.name(),
                                                                 consistencyData.getAverage(),
                                                                 maxOffset,
                                                                 flagOffset))
                                              .setCancelAction(cancelVl, () -> event.setCancelled(true)));
            }

            // Reset the statistics.
            consistencyData.reset();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerFish(final PlayerFishEvent event)
    {
        final var user = de.photon.anticheataddition.user.User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        Log.finer(() -> "AutoFish-Debug | Received fishing event with state " + event.getState());

        switch (event.getState()) {
            case FISHING -> {
                if (!allowAfkFishingFarms) {
                    checkConsistency(TimeKey.AUTOFISH_AFK_DETECTION, user, event);
                }
            }

            case CAUGHT_FISH -> {
                checkConsistency(TimeKey.AUTOFISH_DETECTION, user, event);
                user.getTimeMap().at(TimeKey.AUTOFISH_AFK_DETECTION).update();
            }

            // No consistency when not fishing / failed fishing
            case IN_GROUND, FAILED_ATTEMPT -> {
                user.getTimeMap().at(TimeKey.AUTOFISH_DETECTION).setToZero();
                user.getTimeMap().at(TimeKey.AUTOFISH_AFK_DETECTION).setToZero();
            }

            // CAUGHT_FISH covers all forms of items from the water.
            case BITE -> user.getTimeMap().at(TimeKey.AUTOFISH_DETECTION).update();
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