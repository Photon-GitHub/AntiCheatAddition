package de.photon.anticheataddition.util.violationlevels.threshold;

import de.photon.anticheataddition.util.log.Log;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@RequiredArgsConstructor
final class SingleThresholds implements ThresholdManagement
{
    private final Threshold threshold;

    @Override
    public void executeThresholds(int fromVl, int toVl, @NotNull Player player)
    {
        Log.finer(() -> "SingleThresholds executed: " + 1 + " from " + fromVl + " to " + toVl + " for " + player.getName());
        if (fromVl < threshold.vl() && toVl >= threshold.vl()) threshold.executeCommandList(player);
    }

    @Override
    public List<Threshold> getThresholds()
    {
        return List.of(threshold);
    }
}
