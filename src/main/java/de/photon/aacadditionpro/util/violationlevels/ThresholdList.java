package de.photon.aacadditionpro.util.violationlevels;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import de.photon.aacadditionpro.util.files.configs.ConfigUtils;
import lombok.Getter;
import lombok.val;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.stream.Collectors;

public class ThresholdList
{
    @Getter
    private final NavigableMap<Integer, Threshold> thresholdMap;

    public ThresholdList(List<Threshold> thresholds)
    {
        final ImmutableSortedMap.Builder<Integer, Threshold> builder = ImmutableSortedMap.naturalOrder();
        for (Threshold threshold : thresholds) builder.put(threshold.getVl(), threshold);
        thresholdMap = builder.build();
    }

    /**
     * Returns an empty {@link ThresholdList}.
     */
    public static ThresholdList empty()
    {
        return new ThresholdList(ImmutableList.of());
    }

    /**
     * Tries to load all thresholds from the given config key.
     *
     * @param thresholdSectionPath the given path to the section that contains the thresholds
     *
     * @return a mutable {@link List} containing all {@link Threshold}s.
     */
    public static ThresholdList loadThresholds(final String thresholdSectionPath)
    {
        // Make sure that the keys exist.
        return new ThresholdList(Objects.requireNonNull(ConfigUtils.loadKeys(thresholdSectionPath), "Severe loading error: Keys are null when loading: " + thresholdSectionPath)
                                        .stream()
                                        // Create a new Threshold for every key.
                                        .map(key -> new Threshold(Integer.parseInt(key), ConfigUtils.loadImmutableStringOrStringList(thresholdSectionPath + '.' + key)))
                                        // Collect the keys.
                                        .collect(Collectors.toList()));
    }

    /**
     * Used to execute the commands of the {@link Threshold}s in this  {@link ThresholdList}.
     */
    public void executeThresholds(int fromVl, int toVl, Collection<Player> players)
    {
        val toExecute = thresholdMap.subMap(fromVl, false, toVl, true).values();
        for (Threshold threshold : toExecute) threshold.executeCommandList(players);
    }
}
