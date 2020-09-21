package de.photon.aacadditionpro.modules.checks;

import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.ListenerModule;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.RestrictedServerVersion;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.DataKey;
import de.photon.aacadditionpro.user.TimestampKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.mathematics.MathUtils;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Set;

public class AutoPotion implements ListenerModule, ViolationModule, RestrictedServerVersion
{
    @Getter
    private static final AutoPotion instance = new AutoPotion();

    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 120L);

    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancelVl;

    @LoadFromConfiguration(configPath = ".timeout")
    private int timeout;

    /**
     * How much time can pass between
     * 1) rotation and throwing a potion
     * 2) throwing a potion and backwards-rotation
     * so that the check is NOT failed
     */
    @LoadFromConfiguration(configPath = ".time_offset")
    private int timeOffset;

    /**
     * How much offset is allowed when comparing the previous angle to the angle of the back-rotation to continue the check
     */
    @LoadFromConfiguration(configPath = ".angle_offset")
    private double angleOffset;

    /**
     * The initial pitch-difference, measured in degrees, to start the check
     */
    @LoadFromConfiguration(configPath = ".angle_start_threshold")
    private double angleStartThreshold;

    /**
     * The minimum angle that is counted as looking down
     */
    @LoadFromConfiguration(configPath = ".look_down_angle")
    private double lookDownAngle;

    @EventHandler
    public void onMove(final PlayerMoveEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        if (user.getDataMap().getBoolean(DataKey.AUTOPOTION_ALREADY_THROWN)) {
            // The pitch and yaw values are nearly the same as before
            if (MathUtils.roughlyEquals(event.getTo().getPitch(), user.getDataMap().getFloat(DataKey.AUTOPOTION_LAST_SUDDEN_PITCH), angleOffset) &&
                MathUtils.roughlyEquals(event.getTo().getYaw(), user.getDataMap().getFloat(DataKey.AUTOPOTION_LAST_SUDDEN_YAW), angleOffset) &&
                // Happened in a short time frame
                user.getTimestampMap().recentlyUpdated(TimestampKey.AUTOPOTION_DETECTION, timeOffset))
            {
                // Flag
                vlManager.flag(user.getPlayer(), cancelVl, () ->
                        // Enable timeout when cancel_vl is crossed
                        user.getTimestampMap().updateTimeStamp(TimestampKey.AUTOPOTION_TIMEOUT), () -> user.getTimestampMap().nullifyTimeStamp(TimestampKey.AUTOPOTION_DETECTION));
            }
        } else {
            // The angle_start_threshold is reached
            if (event.getTo().getPitch() - event.getFrom().getPitch() > this.angleStartThreshold &&
                // The previous pitch is not representing looking down
                event.getFrom().getPitch() < lookDownAngle &&
                // The pitch is beyond the lookdown angle
                event.getTo().getPitch() >= lookDownAngle)
            {
                user.getDataMap().setValue(DataKey.AUTOPOTION_LAST_SUDDEN_PITCH, event.getFrom().getPitch());
                user.getDataMap().setValue(DataKey.AUTOPOTION_LAST_SUDDEN_YAW, event.getFrom().getYaw());
                user.getDataMap().setValue(DataKey.AUTOPOTION_ALREADY_THROWN, false);

                user.getTimestampMap().updateTimeStamp(TimestampKey.AUTOPOTION_DETECTION);
            }
        }
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        // Timeout
        if (user.getTimestampMap().recentlyUpdated(TimestampKey.AUTOPOTION_TIMEOUT, timeout)) {
            event.setCancelled(true);
            return;
        }

        // Is the action a right-click that can throw a potion
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) &&
            // The item is a throwable potion
            event.getItem() != null &&
            event.getMaterial() == Material.SPLASH_POTION &&
            // The last sudden movement was not long ago
            user.getTimestampMap().recentlyUpdated(TimestampKey.AUTOPOTION_DETECTION, timeOffset))
        {
            user.getDataMap().setValue(DataKey.AUTOPOTION_ALREADY_THROWN, true);
            // Here the timestamp is used to contain the data of the last splash
            user.getTimestampMap().updateTimeStamp(TimestampKey.AUTOPOTION_DETECTION);
        }
    }

    @Override
    public boolean isSubModule()
    {
        return false;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.AUTO_POTION;
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public Set<ServerVersion> getSupportedVersions()
    {
        return ServerVersion.NON_188_VERSIONS;
    }
}
