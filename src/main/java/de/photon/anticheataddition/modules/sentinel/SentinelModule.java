package de.photon.anticheataddition.modules.sentinel;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.log.Log;
import de.photon.anticheataddition.util.violationlevels.DetectionManagement;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.entity.Player;

public abstract class SentinelModule extends ViolationModule
{
    protected SentinelModule(String restString)
    {
        super("Sentinel." + restString);
    }

    @Override
    protected final ViolationManagement createViolationManagement()
    {
        // -1 decay ticks to never decay.
        return new DetectionManagement(this);
    }

    /**
     * Used to signal the detection of a player.
     * This checks if the player has bypass permissions.
     */
    protected final void detection(Player player)
    {
        final User user = User.getUser(player);
        if (!User.isUserInvalid(user, this)) detection(user);
        else Log.fine(() -> "SentinelModule " + this.getModuleId() + " detected player " + player.getName() + " but user is invalid, skipping detection.");
    }

    protected final void detection(User user)
    {
        this.getManagement().flag(Flag.of(user.getPlayer()));
    }
}
