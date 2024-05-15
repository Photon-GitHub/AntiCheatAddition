package de.photon.anticheataddition.util.violationlevels.threshold;

import com.google.common.collect.ImmutableSortedMap;
import de.photon.anticheataddition.util.log.Log;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.NavigableMap;

final class MultiThresholds implements ThresholdManagement
{
    private final NavigableMap<Integer, Threshold> thresholdMap;

    public MultiThresholds(List<Threshold> thresholds)
    {
        final var builder = ImmutableSortedMap.<Integer, Threshold>naturalOrder();
        for (Threshold threshold : thresholds) builder.put(threshold.vl(), threshold);
        thresholdMap = builder.build();
    }

    @Override
    public void executeThresholds(int fromVl, int toVl, @NotNull Player player)
    {
        final var toExecute = thresholdMap.subMap(fromVl, false, toVl, true).values();
        Log.finer(() -> "Thresholds executed: " + toExecute.size() + " from " + fromVl + " to " + toVl + " for " + player.getName());
        for (Threshold threshold : toExecute) threshold.executeCommandList(player);
    }

    @Override
    public List<Threshold> getThresholds()
    {
        return List.copyOf(thresholdMap.values());
    }
}
