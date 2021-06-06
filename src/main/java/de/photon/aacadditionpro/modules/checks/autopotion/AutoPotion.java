package de.photon.aacadditionpro.modules.checks.autopotion;

import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.DataKey;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.mathematics.MathUtil;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class AutoPotion extends ViolationModule implements Listener
{
    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancelVl;

    @LoadFromConfiguration(configPath = ".timeout")
    private int timeout;

    @LoadFromConfiguration(configPath = ".look_restored_time")
    private int lookRestoredTime;

    @LoadFromConfiguration(configPath = ".angle_offset")
    private double angleOffset;

    @LoadFromConfiguration(configPath = ".initial_pitch_difference")
    private double initalPitchDifference;

    @LoadFromConfiguration(configPath = ".look_down_angle")
    private double lookDownAngle;

    public AutoPotion(String configString)
    {
        super(configString);
    }

    @EventHandler
    public void onMove(final PlayerMoveEvent event)
    {
        val user = User.getUser(event.getPlayer().getUniqueId());
        if (User.isUserInvalid(user, this) || event.getTo() == null) return;

        if (user.getDataMap().getBoolean(DataKey.BooleanKey.AUTOPOTION_ALREADY_THROWN)) {
            // The pitch and yaw values are nearly the same as before
            if (MathUtil.roughlyEquals(event.getTo().getPitch(), user.getDataMap().getFloat(DataKey.FloatKey.AUTOPOTION_LAST_SUDDEN_PITCH), angleOffset) &&
                MathUtil.roughlyEquals(event.getTo().getYaw(), user.getDataMap().getFloat(DataKey.FloatKey.AUTOPOTION_LAST_SUDDEN_YAW), angleOffset) &&
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
            if (event.getTo().getPitch() - event.getFrom().getPitch() > this.initalPitchDifference &&
                // The previous pitch is not representing looking down
                event.getFrom().getPitch() < lookDownAngle &&
                // The pitch is beyond the lookdown angle
                event.getTo().getPitch() >= lookDownAngle)
            {
                user.getDataMap().setFloat(DataKey.FloatKey.AUTOPOTION_LAST_SUDDEN_PITCH, event.getFrom().getPitch());
                user.getDataMap().setFloat(DataKey.FloatKey.AUTOPOTION_LAST_SUDDEN_YAW, event.getFrom().getYaw());
                user.getDataMap().setBoolean(DataKey.BooleanKey.AUTOPOTION_ALREADY_THROWN, false);

                user.getTimestampMap().at(TimestampKey.AUTOPOTION_DETECTION).update();
            }
        }
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent event)
    {
        val user = User.getUser(event.getPlayer().getUniqueId());
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
            user.getDataMap().setBoolean(DataKey.BooleanKey.AUTOPOTION_ALREADY_THROWN, true);
            // Here the timestamp is used to contain the data of the last splash
            user.getTimestampMap().at(TimestampKey.AUTOPOTION_DETECTION).update();
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
        return new ViolationLevelManagement(this, 120L, 50);
    }
}
