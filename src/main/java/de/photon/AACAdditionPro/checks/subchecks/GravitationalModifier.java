package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;
import de.photon.AACAdditionPro.util.world.BlockUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class GravitationalModifier implements AACAdditionProCheck, Listener
{
    private static final int MAX_VELOCITY_CHANGES = 12;

    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getAdditionHackType(), 120);

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (AACAdditionProCheck.isUserInvalid(user))
        {
            return;
        }

        // Time of a check cycle is over
        if (!user.getVelocityChangeData().recentlyUpdated(3000))
        {
            final int additionalChanges = user.getVelocityChangeData().velocityChangeCounter - MAX_VELOCITY_CHANGES;

            if (additionalChanges > 0)
            {
                this.vlManager.flag(user.getPlayer(), additionalChanges, () -> {}, () -> {});
            }

            user.getVelocityChangeData().velocityChangeCounter = 0;
            user.getVelocityChangeData().updateTimeStamp();
        }

        // The player wasn't hurt and got velocity for that.
        if (user.getPlayer().getNoDamageTicks() == 0 &&
            // Recent teleports can cause bugs
            !user.getTeleportData().recentlyUpdated(1000) &&
            // Players can jump up and down more often if there is a block above them
            user.getPlayer().getEyeLocation().getBlock().isEmpty() &&
            BlockUtils.blocksAround(user.getPlayer().getEyeLocation().getBlock(), false) == 0)
        {
            boolean positveVelocity = event.getFrom().getY() < event.getTo().getY();

            if (positveVelocity != user.getVelocityChangeData().positiveVelocity)
            {
                user.getVelocityChangeData().velocityChangeCounter++;
                user.getVelocityChangeData().positiveVelocity = positveVelocity;
            }
        }
    }

    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.GRAVITATIONAL_MODIFIER;
    }
}
