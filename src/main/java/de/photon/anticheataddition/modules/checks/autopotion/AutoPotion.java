package de.photon.anticheataddition.modules.checks.autopotion;

import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.DataKey;
import de.photon.anticheataddition.user.data.TimestampKey;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class AutoPotion extends ViolationModule implements Listener
{
    private final int cancelVl = loadInt(".cancel_vl", 2);
    private final int timeout = loadInt(".timeout", 1000);
    private final int lookRestoredTime = loadInt(".look_restored_time", 150);
    private final double angleOffset = loadDouble(".angle_offset", 5);
    private final double initialPitchDifference = loadDouble(".initial_pitch_difference", 40);
    private final double lookDownAngle = loadDouble(".look_down_angle", 80);

    public AutoPotion()
    {
        super("AutoPotion");
    }

    @EventHandler
    public void onMove(final PlayerMoveEvent event)
    {
        val user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this) || event.getTo() == null) return;

        if (user.getDataMap().getBoolean(DataKey.Bool.AUTOPOTION_ALREADY_THROWN)) {
            // The pitch and yaw values are nearly the same as before
            if (MathUtil.absDiff(event.getTo().getPitch(), user.getDataMap().getFloat(DataKey.Float.AUTOPOTION_LAST_SUDDEN_PITCH)) <= angleOffset &&
                MathUtil.absDiff(event.getTo().getYaw(), user.getDataMap().getFloat(DataKey.Float.AUTOPOTION_LAST_SUDDEN_YAW)) <= angleOffset &&
                // Happened in a short time frame
                user.getTimestampMap().at(TimestampKey.AUTOPOTION_DETECTION).recentlyUpdated(lookRestoredTime))
            {
                // Flag
                this.getManagement().flag(Flag.of(user)
                                              .setAddedVl(50)
                                              // Enable timeout when cancel_vl is crossed
                                              .setCancelAction(cancelVl, () -> user.getTimestampMap().at(TimestampKey.AUTOPOTION_TIMEOUT).update())
                                              .setEventNotCancelledAction(() -> user.getTimestampMap().at(TimestampKey.AUTOPOTION_DETECTION).setToZero()));
            }
        } else {
            // The initial_pitch_difference is reached
            if (event.getTo().getPitch() - event.getFrom().getPitch() > this.initialPitchDifference &&
                // The previous pitch is not representing looking down
                event.getFrom().getPitch() < lookDownAngle &&
                // The pitch is beyond the lookdown angle
                event.getTo().getPitch() >= lookDownAngle)
            {
                user.getDataMap().setFloat(DataKey.Float.AUTOPOTION_LAST_SUDDEN_PITCH, event.getFrom().getPitch());
                user.getDataMap().setFloat(DataKey.Float.AUTOPOTION_LAST_SUDDEN_YAW, event.getFrom().getYaw());
                user.getDataMap().setBoolean(DataKey.Bool.AUTOPOTION_ALREADY_THROWN, false);

                user.getTimestampMap().at(TimestampKey.AUTOPOTION_DETECTION).update();
            }
        }
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent event)
    {
        val user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        // Timeout
        if (user.getTimestampMap().at(TimestampKey.AUTOPOTION_TIMEOUT).recentlyUpdated(timeout)) {
            event.setCancelled(true);
            return;
        }

        // Is the action a right-click that can throw a potion
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) &&
            // The item is a throwable potion
            event.getItem() != null &&
            event.getMaterial() == Material.SPLASH_POTION &&
            // The last sudden movement was not long ago
            user.getTimestampMap().at(TimestampKey.AUTOPOTION_DETECTION).recentlyUpdated(lookRestoredTime))
        {
            user.getDataMap().setBoolean(DataKey.Bool.AUTOPOTION_ALREADY_THROWN, true);
            // Here the timestamp is used to contain the data of the last splash
            user.getTimestampMap().at(TimestampKey.AUTOPOTION_DETECTION).update();
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
}
