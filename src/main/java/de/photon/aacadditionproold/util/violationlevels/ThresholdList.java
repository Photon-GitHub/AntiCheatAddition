package de.photon.aacadditionproold.util.violationlevels;

import com.google.common.collect.ImmutableList;
import de.photon.aacadditionproold.ServerVersion;
import de.photon.aacadditionproold.util.exceptions.UnknownMinecraftVersion;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

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
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                Collections.sort(new ArrayList<>(thresholds));
                this.thresholds = ImmutableList.copyOf(thresholds);
                break;
            case MC112:
            case MC113:
            case MC114:
            case MC115:
            case MC116:
                this.thresholds = ImmutableList.sortedCopyOf(thresholds);
                break;
            default:
                throw new UnknownMinecraftVersion();
        }

        vls = new int[thresholds.size()];
        for (int i = 0; i < thresholds.size(); ++i) {
            vls[i] = thresholds.get(i).getVl();
        }
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
    public void forEachThreshold(int fromVl, int toVl, Consumer<Threshold> execute)
    {
        final int fromIndex = this.getFromIndex(fromVl);
        final int toIndex = this.getToIndex(toVl);

        // There are no thresholds between fromVl and toVl.
        if (fromIndex > toIndex) return;

        // <= is intentional here.
        for (int i = fromIndex; i <= toIndex; ++i) {
            execute.accept(this.thresholds.get(i));
        }
    }
}
