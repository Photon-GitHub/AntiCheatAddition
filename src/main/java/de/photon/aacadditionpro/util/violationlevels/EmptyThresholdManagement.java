package de.photon.aacadditionpro.util.violationlevels;

import org.bukkit.entity.Player;

import java.util.Collection;

public class EmptyThresholdManagement implements ThresholdManagement
{
    @Override
    public void executeThresholds(int fromVl, int toVl, Collection<Player> players)
    {
        // Nothing.
    }
}
