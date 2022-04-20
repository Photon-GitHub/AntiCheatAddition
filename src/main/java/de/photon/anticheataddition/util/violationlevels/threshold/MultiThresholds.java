package de.photon.anticheataddition.util.violationlevels.threshold;

import com.google.common.collect.ImmutableSortedMap;
import lombok.Getter;
import lombok.val;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.NavigableMap;

final class MultiThresholds implements ThresholdManagement
{
    @Getter private final NavigableMap<Integer, Threshold> thresholdMap;

    public MultiThresholds(List<Threshold> thresholds)
    {
        val builder = ImmutableSortedMap.<Integer, Threshold>naturalOrder();
        for (Threshold threshold : thresholds) builder.put(threshold.getVl(), threshold);
        thresholdMap = builder.build();
    }

    @Override
    public void executeThresholds(int fromVl, int toVl, @NotNull Player player)
    {
        val toExecute = thresholdMap.subMap(fromVl, false, toVl, true).values();
        for (Threshold threshold : toExecute) threshold.executeCommandList(player);
    }
}
