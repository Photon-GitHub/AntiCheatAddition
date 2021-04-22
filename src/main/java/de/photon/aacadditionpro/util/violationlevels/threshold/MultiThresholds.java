package de.photon.aacadditionpro.util.violationlevels.threshold;

import com.google.common.collect.ImmutableSortedMap;
import lombok.Getter;
import lombok.val;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;

class MultiThresholds implements ThresholdManagement
{
    @Getter private final NavigableMap<Integer, Threshold> thresholdMap;

    public MultiThresholds(List<Threshold> thresholds)
    {
        final ImmutableSortedMap.Builder<Integer, Threshold> builder = ImmutableSortedMap.naturalOrder();
        for (Threshold threshold : thresholds) builder.put(threshold.getVl(), threshold);
        thresholdMap = builder.build();
    }

    @Override
    public void executeThresholds(int fromVl, int toVl, Collection<Player> players)
    {
        val toExecute = thresholdMap.subMap(fromVl, false, toVl, true).values();
        for (Threshold threshold : toExecute) threshold.executeCommandList(players);
    }
}
