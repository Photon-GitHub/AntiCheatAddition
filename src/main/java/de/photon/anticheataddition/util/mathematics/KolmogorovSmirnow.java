package de.photon.anticheataddition.util.mathematics;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Longs;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.stream.IntStream;

@UtilityClass
public final class KolmogorovSmirnow
{
    /**
     * Normalizes the given array of values to the range [0, 1].
     *
     * @param data The array of values to be normalized.
     *
     * @return A new array with normalized values.
     */
    public static double[] normalizeData(double[] data)
    {
        if (data.length == 0) return new double[0];

        final double min = Doubles.min(data);
        final double max = Doubles.max(data);

        if (min == max) return new double[data.length];

        return Arrays.stream(data)
                     .map(offset -> (offset - min) / (max - min))
                     .toArray();
    }

    /**
     * Normalizes the given array of values to the range [0, 1].
     *
     * @param data The array of values to be normalized.
     *
     * @return A new array with normalized values.
     */
    public static double[] normalizeData(long[] data)
    {
        if (data.length == 0) return new double[0];

        final double min = Longs.min(data);
        final double max = Longs.max(data);

        if (min == max) return new double[data.length];

        return Arrays.stream(data)
                     .mapToDouble(offset -> (offset - min) / (max - min))
                     .toArray();
    }

    /**
     * Calculates the Kolmogorov-Smirnov statistic for the given sample, assuming a uniform distribution.
     *
     * @param sample The sample to be tested.
     *
     * @return The D statistic, ranging from 0 to 1.
     */
    public double kSTestForUniformDistribution(double[] sample)
    {
        if (sample == null || sample.length == 0) return 1.0;

        Arrays.sort(sample);

        // Calculate the empirical distribution function (EDF) and the CDF
        return IntStream.range(0, sample.length)
                        .mapToDouble(i -> {
                            double edf = (double) (i + 1) / sample.length;
                            double cdf = sample[i]; // CDF of U(0, 1) is the identity function
                            return Math.abs(edf - cdf);
                        })
                        .max()
                        .orElse(1.0);
    }
}