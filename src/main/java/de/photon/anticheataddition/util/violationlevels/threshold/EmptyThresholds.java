package de.photon.anticheataddition.util.violationlevels.threshold;

import de.photon.anticheataddition.util.log.Log;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class EmptyThresholds implements ThresholdManagement
{
    @Override
    public void executeThresholds(int fromVl, int toVl, @NotNull Player player)
    {
        Log.finest(() -> "EmptyThresholds executed: " + 0 + " from " + fromVl + " to " + toVl + " for " + player.getName());
        // Do nothing, as there are no thresholds.
    }

    @Override
    public List<Threshold> getThresholds()
    {
        return List.of();
    }
}
