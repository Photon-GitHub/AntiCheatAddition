package de.photon.anticheataddition.modules.sentinel;

import de.photon.anticheataddition.modules.ViolationModule;
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
    protected ViolationManagement createViolationManagement()
    {
        // -1 decay ticks to never decay.
        return new DetectionManagement(this);
    }

    /**
     * Used to signal the detection of a player.
     */
    protected void detection(Player player)
    {
        this.getManagement().flag(Flag.of(player));
    }
}
