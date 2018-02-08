package de.photon.AACAdditionPro.util.storage.management;

import java.util.DoubleSummaryStatistics;

public class DoubleBuffer extends Buffer<Double>
{
    public DoubleBuffer(int buffer_size)
    {
        super(buffer_size);
    }

    /**
     * Adds all the {@link Double}s in this {@link DoubleBuffer}.
     *
     * @return the sum of all elements in this {@link DoubleBuffer}.
     */
    public double sum()
    {
        double result = 0D;
        for (double d : this)
        {
            result += d;
        }
        return result;
    }

    /**
     * @return the maximum double in this {@link DoubleBuffer}
     */
    public double max()
    {
        double max = Double.MIN_VALUE;
        for (double d : this)
        {
            if (d > max)
            {
                max = d;
            }
        }
        return max;
    }

    /**
     * @return the minimum double in this {@link DoubleBuffer}
     */
    public double min()
    {
        double min = Double.MAX_VALUE;
        for (double d : this)
        {
            if (d < min)
            {
                min = d;
            }
        }
        return min;
    }

    /**
     * @return the average of all {@link Double}s in this {@link Buffer}
     */
    public double average()
    {
        return this.sum() / this.size();
    }

    /**
     * Clears the {@link DoubleBuffer} whilst creating a {@link DoubleSummaryStatistics}
     *
     * @return the {@link DoubleSummaryStatistics} which has been created.
     */
    public DoubleSummaryStatistics clearSummary()
    {
        final DoubleSummaryStatistics doubleSummaryStatistics = new DoubleSummaryStatistics();
        this.clearLastObjectIteration(doubleSummaryStatistics::accept);
        return doubleSummaryStatistics;
    }
}
