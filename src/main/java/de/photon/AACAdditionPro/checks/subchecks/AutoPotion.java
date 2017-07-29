package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import de.photon.AACAdditionPro.util.multiversion.ServerVersion;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AutoPotion implements Listener, AACAdditionProCheck
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getAdditionHackType(), 120L);

    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancel_vl;

    @LoadFromConfiguration(configPath = ".timeout")
    private int timeout;

    /**
     * How much time can pass between
     * 1) rotation and throwing a potion
     * 2) throwing a potion and backwards-rotation
     * so that the check is NOT failed
     */
    @LoadFromConfiguration(configPath = ".time_offset")
    private int time_offset;

    /**
     * How much offset is allowed when comparing the previous angle to the angle of the back-rotation to continue the check
     */
    @LoadFromConfiguration(configPath = ".angle_offset")
    private double angle_offset;

    /**
     * The initial pitch-difference, measured in degrees, to start the check
     */
    @LoadFromConfiguration(configPath = ".angle_start_threshold")
    private double angle_start_threshold;

    /**
     * The minimum angle that is counted as looking down
     */
    @LoadFromConfiguration(configPath = ".look_down_angle")
    private double look_down_angle;

    @EventHandler
    public void on(final PlayerMoveEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (user == null || user.isBypassed()) {
            return;
        }

        if (user.getAutoPotionData().alreadyThrown) {
            // The pitch and yaw values are nearly the same as before
            if (MathUtils.isInRange(event.getTo().getPitch(), user.getAutoPotionData().lastSuddenPitch, angle_offset) &&
                MathUtils.isInRange(event.getTo().getYaw(), user.getAutoPotionData().lastSuddenYaw, angle_offset) &&
                // Happened in a short timeframe
                user.getAutoPotionData().recentlyUpdated(0, time_offset))
            {
                // Flag
                vlManager.flag(user.getPlayer(), cancel_vl, () -> {
                    // Enable timeout when cancel_vl is crossed
                    user.getAutoPotionData().updateTimeStamp(1);
                }, () -> user.getAutoPotionData().nullifyTimeStamp(0));
            }
        } else {
            // The angle_start_threshold is reached
            if (event.getTo().getPitch() - event.getFrom().getPitch() > this.angle_start_threshold &&
                // The previous pitch is not representing looking down
                event.getFrom().getPitch() < look_down_angle &&
                // The pitch is beyond the lookdown angle
                event.getTo().getPitch() >= look_down_angle)
            {
                user.getAutoPotionData().lastSuddenPitch = event.getFrom().getPitch();
                user.getAutoPotionData().lastSuddenYaw = event.getFrom().getYaw();
                user.getAutoPotionData().alreadyThrown = false;

                // Index 0 is reserved for AutoPotion's internal stuff
                user.getAutoPotionData().updateTimeStamp(0);
            }
        }
    }

    @EventHandler
    public void on(final PlayerInteractEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (AACAdditionProCheck.isUserInvalid(user)) {
            return;
        }

        // Timeout
        if (user.getAutoPotionData().recentlyUpdated(1, timeout)) {
            event.setCancelled(true);
            return;
        }

        // Is the action a right-click that can throw a potion
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) &&
            // The item is a throwable potion
            event.getItem() != null &&
            event.getMaterial() == Material.SPLASH_POTION &&
            // The last sudden movement was not long ago
            user.getAutoPotionData().recentlyUpdated(0, time_offset))
        {
            user.getAutoPotionData().alreadyThrown = true;
            // Here the timestamp is used to contain the data of the last splash
            user.getAutoPotionData().updateTimeStamp(0);
        }
    }

    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.AUTO_POTION;
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public Set<ServerVersion> getSupportedVersions()
    {
        return new HashSet<>(Arrays.asList(ServerVersion.MC110, ServerVersion.MC111, ServerVersion.MC112));
    }
}
