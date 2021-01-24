package de.photon.aacadditionpro.util.violationlevels;

import com.google.common.collect.ImmutableList;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.util.files.configs.ConfigUtils;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ThresholdList
{
    /**
     * A {@link List} of {@link Threshold}s which is guaranteed to be sorted.
     */
    @Getter
    private final List<Threshold> thresholds;
    // Sorted set of the vls of the Thresholds for fast index evaluation.
    private final int[] vls;

    public ThresholdList(List<Threshold> thresholds)
    {
        if (ServerVersion.supportsActiveServerVersion(ServerVersion.NON_188_VERSIONS)) {
            List<Threshold> temp = new ArrayList<>(thresholds);
            Collections.sort(temp);
            this.thresholds = ImmutableList.copyOf(temp);
        } else {
            this.thresholds = ImmutableList.sortedCopyOf(thresholds);
        }

        vls = new int[thresholds.size()];
        for (int i = 0; i < vls.length; ++i) {
            vls[i] = thresholds.get(i).getVl();
        }
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
     * Calculates the index of fromVl + 1 or the next higher index if not present.
     * + 1 because we do not want to punish the current vl.
     */
    public int getFromIndex(int fromVl)
    {
        int index = Arrays.binarySearch(this.vls, fromVl + 1);
        if (index < 0) index = -(index + 1);
        return index;
    }

    /**
     * Calculates the index of toVl or the next lower index if not present.
     */
    public int getToIndex(int toVl)
    {
        int index = Arrays.binarySearch(this.vls, toVl);
        // +2 as that will allow us to drop one decrement operation afterwards to floor the result.
        if (index < 0) index = -(index + 2);
        return index;
    }

    /**
     * Used to execute the commands of the {@link Threshold}s in this  {@link ThresholdList}.
     */
    public void executeThresholds(int fromVl, int toVl, Collection<Player> players)
    {
        final int fromIndex = this.getFromIndex(fromVl);
        final int toIndex = this.getToIndex(toVl);

        // There are no thresholds between fromVl and toVl.
        if (fromIndex > toIndex) return;

        // <= is intentional here.
        for (int i = fromIndex; i <= toIndex; ++i) {
            this.thresholds.get(i).executeCommandList(players);
        }
    }
}
