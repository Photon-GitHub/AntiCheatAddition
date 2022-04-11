package de.photon.anticheataddition.util.datastructure.statistics;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.util.mathematics.ModularInteger;
import lombok.Getter;

import java.util.Arrays;

/**
 * This class represents a statistic that has a fixed length.
 * The oldest element is overwritten.
 * <p>
 * Removing elements is not supported.
 * This class guarantees O(1) operations for add, sum and average.
 */
public final class MovingLongStatistics
{
    private final long[] data;
    private final ModularInteger index;
    @Getter private long sum = 0;
    @Getter private double average = 0;

    public MovingLongStatistics(int capacity)
    {
        Preconditions.checkArgument(capacity > 0, "Tried to create MovingLongStatistics with 0 or negative capacity.");
        this.data = new long[capacity];
        this.index = new ModularInteger(0, capacity);
    }

    public MovingLongStatistics(int capacity, long defaultValue)
    {
        this(capacity);
        Arrays.fill(this.data, defaultValue);
        this.sum = Math.multiplyExact(capacity, defaultValue);
        this.average = defaultValue;
    }

    /**
     * This method adds a new datapoint to the floating sum and average.
     * Long overflows are not detected by this method!
     */
    public void add(long value)
    {
        final int effIndex = index.getAndIncrement();
        // Remove the overwritten element from the sum

        sum -= data[effIndex];
        // Actually overwrite.
        data[effIndex] = value;
        // Add the new element to the sum.
        sum += value;

        // Update the average.
        average = sum / (double) data.length;
    }
}
