package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import de.photon.AACAdditionPro.util.world.BlockUtils;
import me.konsolas.aac.api.AACAPIProvider;
import me.konsolas.aac.api.HackType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class GravitationalModifier implements AACAdditionProCheck, Listener
{
    private static final int MAX_VELOCITY_CHANGES = 12;

    @LoadFromConfiguration(configPath = ".weight")
    private double weight;

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
                final int additionalVl = (int) (weight * additionalChanges);

                VerboseSender.sendVerboseMessage("Player " + event.getPlayer().getName() + " failed GravitationalModifer: " + additionalChanges + " additional changes | added VL: " + additionalVl);
                AACAPIProvider.getAPI().setViolationLevel(
                        user.getPlayer(),
                        HackType.SPEED,
                        AACAPIProvider.getAPI().getViolationLevel(user.getPlayer(), HackType.SPEED) + additionalVl);
            }

            user.getVelocityChangeData().velocityChangeCounter = 0;
            user.getVelocityChangeData().updateTimeStamp();
        }

        // The player wasn't hurt and got velocity for that.
        if (user.getPlayer().getNoDamageTicks() == 0 &&
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
