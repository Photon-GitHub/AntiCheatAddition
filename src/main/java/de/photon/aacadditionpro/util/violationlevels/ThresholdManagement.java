package de.photon.aacadditionpro.util.violationlevels;

import org.bukkit.entity.Player;

import java.util.Collection;

public interface ThresholdManagement
{
    /**
     * Used to execute the commands of the {@link Threshold}s in this  {@link ThresholdMapManagement}.
     */
    void executeThresholds(int fromVl, int toVl, Collection<Player> players);
}
