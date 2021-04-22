package de.photon.aacadditionpro.util.violationlevels.threshold;

import org.bukkit.entity.Player;

import java.util.Collection;

class EmptyThresholds extends ThresholdManagement
{
    @Override
    public void executeThresholds(int fromVl, int toVl, Collection<Player> players)
    {
        // Nothing.
    }
}
