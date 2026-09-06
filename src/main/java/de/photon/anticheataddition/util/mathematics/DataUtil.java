package de.photon.anticheataddition.util.mathematics;

import com.google.common.base.Preconditions;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Utility class providing common operations for numerical data arrays,
 * including sum, average, variance, and outlier removal.
 * <p>
 * Supports primitive types int, long, and double.
 */
@UtilityClass
public final class DataUtil {

    /**
     * Calculates the sum of the given int values.
     *
     * @param data the ints to sum
     * @return the total sum of the provided values
     */
    public static int sum(int... data)
    {
        int sum = 0;
        for (int datum : data) sum += datum;
        return sum;
    }

    /**
     * Calculates the sum of the given long values.
     *
     * @param data the longs to sum
     * @return the total sum of the provided values
     */
    public static long sum(long... data)
    {
        long sum = 0L;
        for (long datum : data) sum += datum;
        return sum;
    }

    /**
     * Calculates the sum of the given double values.
     *
     * @param data the doubles to sum
     * @return the total sum of the provided values
     */
    public static double sum(double... data)
    {
        double sum = 0.0;
        for (double datum : data) sum += datum;
        return sum;
    }

    /**
     * Computes the arithmetic mean (average) of the given int values.
     *
     * @param data the ints to average
     * @return the mean value as a double
     * @throws IllegalArgumentException if {@code data.length == 0}
     */
    public static double average(int... data)
    {
        Preconditions.checkArgument(data.length > 0, "Cannot compute average of zero elements.");
        return sum(data) / (double) data.length;
    }

    /**
     * Computes the arithmetic mean (average) of the given long values.
     *
     * @param data the longs to average
     * @return the mean value as a double
     * @throws IllegalArgumentException if {@code data.length == 0}
     */
    public static double average(long... data)
    {
        Preconditions.checkArgument(data.length > 0, "Cannot compute average of zero elements.");
        return sum(data) / (double) data.length;
    }

    /**
     * Computes the arithmetic mean (average) of the given double values.
     *
     * @param data the doubles to average
     * @return the mean value as a double
     * @throws IllegalArgumentException if {@code data.length == 0}
     */
    public static double average(double... data)
    {
        Preconditions.checkArgument(data.length > 0, "Cannot compute average of zero elements.");
        return sum(data) / data.length;
    }

    /**
     * Computes the squared error between a reference and a given value.
     * <p>
     * This is effectively the variance contribution of a single observation.
     *
     * @param reference the reference or expected value
     * @param value     the observed value
     * @return the squared difference {@code (value - reference)^2}
     */
    public static double variance(final double reference, final double value)
    {
        final double error = value - reference;
        return error * error;
    }

    /**
     * Calculates the summed squared error of int values from a given reference.
     *
     * @param reference the reference value to compare against
     * @param data      the int values to evaluate
     * @return the sum of squared differences
     */
    public static double variance(double reference, int... data)
    {
        double sum = 0.0;
        for (int datum : data) sum += variance(reference, datum);
        return sum;
    }

    /**
     * Calculates the summed squared error of long values from a given reference.
     *
     * @param reference the reference value to compare against
     * @param data      the long values to evaluate
     * @return the sum of squared differences
     */
    public static double variance(double reference, long... data)
    {
        double sum = 0.0;
        for (long datum : data) sum += variance(reference, datum);
        return sum;
    }

    /**
     * Calculates the summed squared error of double values from a given reference.
     *
     * @param reference the reference value to compare against
     * @param data      the double values to evaluate
     * @return the sum of squared differences
     */
    public static double variance(double reference, double... data)
    {
        double sum = 0.0;
        for (double datum : data) sum += variance(reference, datum);
        return sum;
    }

    /**
     * Removes the specified number of farthest int elements (outliers) from the mean.
     * <p>
     * Converts the array to a stream, sorts by distance from the mean, and drops
     * the largest {@code numberOutliers} distances. The order of the remaining elements
     * is not guaranteed to match the original.
     *
     * @param numberOutliers the count of farthest elements to remove
     * @param data           the int array to process
     * @return a new array containing the remaining elements
     * @throws IllegalArgumentException if {@code data} is null or too small
     */
    public static int[] removeOutliers(int numberOutliers, int... data)
    {
        Preconditions.checkArgument(data != null, "Data must not be null.");
        Preconditions.checkArgument(numberOutliers >= 0, "Number of outliers must not be negative.");
        Preconditions.checkArgument(data.length > numberOutliers, "Not enough data to remove outliers.");

        final double mean = average(data);
        return Arrays.stream(data)
                     .boxed()
                     // Sort the data based on their distance from the mean
                     .sorted(Comparator.comparingDouble(d -> Math.abs(d - mean)))
                     // Remove the specified number of outliers with the highest distance
                     .limit(data.length - numberOutliers)
                     // Convert the remaining elements in the stream to an array
                     .mapToInt(Integer::intValue)
                     .toArray();
    }

    /**
     * Removes the specified number of farthest long elements (outliers) from the mean.
     * <p>
     * Converts the array to a stream, sorts by distance from the mean, and drops
     * the largest {@code numberOutliers} distances. The order of the remaining elements
     * is not guaranteed to match the original.
     *
     * @param numberOutliers the count of farthest elements to remove
     * @param data           the long array to process
     * @return a new array containing the remaining elements
     * @throws IllegalArgumentException if {@code data} is null or too small
     */
    public static long[] removeOutliers(int numberOutliers, long... data)
    {
        Preconditions.checkArgument(data != null, "Data must not be null.");
        Preconditions.checkArgument(numberOutliers >= 0, "Number of outliers must not be negative.");
        Preconditions.checkArgument(data.length > numberOutliers, "Not enough data to remove outliers.");

        final double mean = average(data);
        return Arrays.stream(data)
                     .boxed()
                     // Sort the data based on their distance from the mean
                     .sorted(Comparator.comparingDouble(d -> Math.abs(d - mean)))
                     // Remove the specified number of outliers with the highest distance
                     .limit(data.length - numberOutliers)
                     // Convert the remaining elements in the stream to an array
                     .mapToLong(Long::longValue)
                     .toArray();
    }

    /**
     * Removes the specified number of farthest double elements (outliers) from the mean.
     * <p>
     * Converts the array to a stream, sorts by distance from the mean, and drops
     * the largest {@code numberOutliers} distances. The order of the remaining elements
     * is not guaranteed to match the original.
     *
     * @param numberOutliers the count of farthest elements to remove
     * @param data           the double array to process
     * @return a new array containing the remaining elements
     * @throws IllegalArgumentException if {@code data} is null or too small
     */
    public static double[] removeOutliers(int numberOutliers, double... data)
    {
        Preconditions.checkArgument(data != null, "Data must not be null.");
        Preconditions.checkArgument(numberOutliers >= 0, "Number of outliers must not be negative.");
        Preconditions.checkArgument(data.length > numberOutliers, "Not enough data to remove outliers.");

        final double mean = average(data);
        return Arrays.stream(data)
                     .boxed()
                     // Sort the data based on their distance from the mean
                     .sorted(Comparator.comparingDouble(d -> Math.abs(d - mean)))
                     // Remove the specified number of outliers with the highest distance
                     .limit(data.length - numberOutliers)
                     // Convert the remaining elements in the stream to an array
                     .mapToDouble(Double::doubleValue)
                     .toArray();
    }

    /** Median of a copy; returns NaN for an empty sample and leaves the input unchanged. */
    public static double median(final double[] values)
    {
        if (values.length == 0) return Double.NaN;
        final double[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        final int middle = sorted.length / 2;
        return (sorted.length & 1) == 0 ? (sorted[middle - 1] + sorted[middle]) * 0.5D : sorted[middle];
    }

    /** Sample standard deviation divided by the absolute mean; NaN for insufficient or near-zero-mean data. */
    public static double coefficientOfVariation(final double[] values)
    {
        if (values.length < 2) return Double.NaN;
        final double mean = average(values);
        if (!Double.isFinite(mean) || Math.abs(mean) <= 1E-9D) return Double.NaN;
        return sampleStandardDeviation(values) / Math.abs(mean);
    }

    /** Sample standard deviation (n - 1 normalization), or NaN for fewer than two samples. */
    public static double sampleStandardDeviation(final double[] values)
    {
        if (values.length < 2) return Double.NaN;
        return Math.sqrt(variance(average(values), values) / (values.length - 1D));
    }

    /** Pearson correlation; returns zero when fewer than two pairs or a constant series provide no evidence. */
    public static double correlation(final double[] first, final double[] second)
    {
        return correlation(first, second, 0);
    }

    /**
     * Pearson correlation of first[i] and second[i + lag] over the overlapping samples.
     * Arrays must have equal lengths and lag must be nonnegative. Insufficient or effectively constant samples
     * (product of centered L2 norms at most 1E-12) return zero rather than inventing correlation evidence.
     */
    public static double correlation(final double[] first, final double[] second, final int lag)
    {
        if (first.length != second.length) throw new IllegalArgumentException("series must have equal lengths");
        if (lag < 0) throw new IllegalArgumentException("lag must not be negative");
        final int length = first.length - lag;
        if (length <= 1) return 0D;

        double firstMean = 0D;
        double secondMean = 0D;
        for (int i = 0; i < length; i++) {
            firstMean += first[i];
            secondMean += second[i + lag];
        }
        firstMean /= length;
        secondMean /= length;

        double covariance = 0D;
        double firstVariance = 0D;
        double secondVariance = 0D;
        for (int i = 0; i < length; i++) {
            final double centeredFirst = first[i] - firstMean;
            final double centeredSecond = second[i + lag] - secondMean;
            covariance += centeredFirst * centeredSecond;
            firstVariance += centeredFirst * centeredFirst;
            secondVariance += centeredSecond * centeredSecond;
        }

        final double denominator = Math.sqrt(firstVariance * secondVariance);
        return denominator <= 1E-12D ? 0D : Math.clamp(covariance / denominator, -1D, 1D);
    }

}
