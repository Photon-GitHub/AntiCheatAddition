package de.photon.anticheataddition.util.violationlevels.threshold;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

final class EmptyThresholds implements ThresholdManagement
{
    @Override
    public void executeThresholds(int fromVl, int toVl, @NotNull Player players)
    {
        // Do nothing, as there are no thresholds.
    }
}
