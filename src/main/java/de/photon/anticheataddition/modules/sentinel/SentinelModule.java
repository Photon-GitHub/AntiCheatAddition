package de.photon.anticheataddition.modules.sentinel;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.util.violationlevels.DetectionManagement;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.entity.Player;

import java.util.UUID;

public abstract class SentinelModule extends ViolationModule
{
    private static final int AAC_FEATURE_SCORE = AntiCheatAddition.getInstance().getConfig().getInt("Sentinel.AACFeatureScore");

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

    @Override
    public double getAACScore(UUID uuid)
    {
        return this.getManagement().getVL(uuid) == 0 ? 0 : AAC_FEATURE_SCORE;
    }
}