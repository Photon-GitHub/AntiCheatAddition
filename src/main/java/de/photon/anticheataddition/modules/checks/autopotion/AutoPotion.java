package de.photon.anticheataddition.modules.checks.autopotion;

import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class AutoPotion extends ViolationModule implements Listener
{
    public static final AutoPotion INSTANCE = new AutoPotion();

    private final int cancelVl = loadInt(".cancel_vl", 2);
    private final int timeout = loadInt(".timeout", 1000);
    private static final int TIME_TO_LOOK_UP_AGAIN = 200;
    private static final double ANGLE_DIFF_FROM_PREVIOUS_POSITION = 5;
    private static final double LARGE_PITCH_CHANGE_THRESHOLD = 40;
    // The pitch in degrees below which a player is considered looking down.
    private static final double LOOKING_DOWN_THRESHOLD_ANGLE = 80;

    private AutoPotion()
    {
        super("AutoPotion");
    }

    @EventHandler
    public void onMove(final PlayerMoveEvent event)
    {
        final var user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this) || event.getTo() == null) return;

        switch (user.getData().object.autoPotionState) {
            // After the potion was thrown, we check if the move event recovers the previous pitch and yaw before the sudden movement and potion throw.
            case POTION_THROWN -> {
                // This should happen very fast, otherwise a normal player can trigger it.
                // If we are past the time to recover the pitch, just search for new throws.
                if (user.getTimeMap().at(TimeKey.AUTOPOTION_SUDDEN_MOVEMENT).recentlyUpdated(TIME_TO_LOOK_UP_AGAIN)) {
                    // Now to check if the yaw and pitch were actually recovered.
                    if (MathUtil.absDiff(event.getTo().getPitch(), user.getData().floating.autopotionBeforeLastSuddenPitch) <= ANGLE_DIFF_FROM_PREVIOUS_POSITION &&
                        MathUtil.yawDistance(event.getTo().getYaw(), user.getData().floating.autopotionBeforeLastSuddenYaw) <= ANGLE_DIFF_FROM_PREVIOUS_POSITION) {
                        this.getManagement().flag(Flag.of(user)
                                                      .setAddedVl(50)
                                                      // Enable timeout when cancel_vl is crossed
                                                      .setCancelAction(cancelVl, () -> user.getTimeMap().at(TimeKey.AUTOPOTION_TIMEOUT).update())
                                                      .setEventNotCancelledAction(() -> user.getTimeMap().at(TimeKey.AUTOPOTION_SUDDEN_MOVEMENT).setToZero()));
                    }
                } else {
                    user.getData().object.autoPotionState = AutoPotionState.AWAIT_POTION_THROW;
                }
            }

            case AWAIT_POTION_THROW -> {
                // Check if there is a large change in the pitch value (bot-like), and if the direction of the head movement is downwards (= higher pitch).
                // Only if the head movement is downwards, this will be greater than 0.
                if (event.getTo().getPitch() - event.getFrom().getPitch() > LARGE_PITCH_CHANGE_THRESHOLD &&
                    // getFrom() is not looking down, but getTo() is.
                    event.getFrom().getPitch() < LOOKING_DOWN_THRESHOLD_ANGLE &&
                    event.getTo().getPitch() >= LOOKING_DOWN_THRESHOLD_ANGLE) {
                    // Save the old pitch and yaw before looking down to check if they are recovered later.
                    user.getData().floating.autopotionBeforeLastSuddenPitch = event.getFrom().getPitch();
                    user.getData().floating.autopotionBeforeLastSuddenYaw = event.getFrom().getYaw();

                    user.getTimeMap().at(TimeKey.AUTOPOTION_SUDDEN_MOVEMENT).update();
                }
            }
        }
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent event)
    {
        final var user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        // Timeout
        if (user.getTimeMap().at(TimeKey.AUTOPOTION_TIMEOUT).recentlyUpdated(timeout)) {
            event.setCancelled(true);
            return;
        }

        // Check if the player threw a splash potion with this interact event, and that potion use was after a sudden head movement.
        // If that is so, set the state to alreadyThrown and update the detection time map.
        switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> {
                if (event.getItem() != null && event.getMaterial() == Material.SPLASH_POTION &&
                    // The last sudden movement was not long ago
                    user.getTimeMap().at(TimeKey.AUTOPOTION_SUDDEN_MOVEMENT).recentlyUpdated(TIME_TO_LOOK_UP_AGAIN)) {
                    // Now check that potion throw.
                    user.getData().object.autoPotionState = AutoPotionState.POTION_THROWN;
                }
            }
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
        return ViolationLevelManagement.builder(this).loadThresholdsToManagement().withDecay(120, 50).build();
    }

    public enum AutoPotionState
    {
        AWAIT_POTION_THROW,
        POTION_THROWN,
    }
}
