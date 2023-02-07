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
import lombok.val;
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
    private final int lookRestoredTime = loadInt(".look_restored_time", 200);
    private final double angleOffset = loadDouble(".angle_offset", 5);
    private final double initialPitchDifference = loadDouble(".initial_pitch_difference", 40);
    private final double lookDownAngle = loadDouble(".look_down_angle", 80);

    private AutoPotion()
    {
        super("AutoPotion");
    }

    @EventHandler
    public void onMove(final PlayerMoveEvent event)
    {
        val user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this) || event.getTo() == null) return;

        switch (user.getData().object.autoPotionState) {
            // After the potion was thrown, we check if the move event recovers the previous pitch and yaw before the sudden movement and potion throw.
            case POTION_THROWN -> {
                // This should happen very fast, otherwise a normal player can trigger it.
                // If we are past the time to recover the pitch, just search for new throws.
                if (user.getTimeMap().at(TimeKey.AUTOPOTION_SUDDEN_MOVEMENT).recentlyUpdated(lookRestoredTime)) {
                    // Now to check if the yaw and pitch were actually recovered.
                    if (MathUtil.absDiff(event.getTo().getPitch(), user.getData().floating.autopotionBeforeLastSuddenPitch) <= angleOffset &&
                        MathUtil.absDiff(event.getTo().getYaw(), user.getData().floating.autopotionBeforeLastSuddenYaw) <= angleOffset)
                    {
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
                if (event.getTo().getPitch() - event.getFrom().getPitch() > this.initialPitchDifference &&
                    // getFrom() is not looking down, but getTo() is.
                    event.getFrom().getPitch() < lookDownAngle &&
                    event.getTo().getPitch() >= lookDownAngle)
                {
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
        val user = User.getUser(event.getPlayer());
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
                    user.getTimeMap().at(TimeKey.AUTOPOTION_SUDDEN_MOVEMENT).recentlyUpdated(lookRestoredTime))
                {
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
