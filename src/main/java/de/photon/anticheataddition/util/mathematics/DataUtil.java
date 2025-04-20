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
public final class DataUtil
{

    /**
     * Calculates the sum of the given int values.
     *
     * @param data the ints to sum
     *
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
     *
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
     *
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
     *
     * @return the mean value as a double
     *
     * @throws ArithmeticException if {@code data.length == 0}
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
     *
     * @return the mean value as a double
     *
     * @throws ArithmeticException if {@code data.length == 0}
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
     *
     * @return the mean value as a double
     *
     * @throws ArithmeticException if {@code data.length == 0}
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
     *
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
     *
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
     *
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
     *
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
     *
     * @return a new array containing the remaining elements
     *
     * @throws IllegalArgumentException if {@code data} is null or too small
     */
    public static int[] removeOutliers(int numberOutliers, int... data)
    {
        if (data == null || data.length <= numberOutliers) throw new IllegalArgumentException("Not enough data to remove outliers.");

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
     *
     * @return a new array containing the remaining elements
     *
     * @throws IllegalArgumentException if {@code data} is null or too small
     */
    public static long[] removeOutliers(int numberOutliers, long... data)
    {
        if (data == null || data.length <= numberOutliers) throw new IllegalArgumentException("Not enough data to remove outliers.");

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
     *
     * @return a new array containing the remaining elements
     *
     * @throws IllegalArgumentException if {@code data} is null or too small
     */
    public static double[] removeOutliers(int numberOutliers, double... data)
    {
        if (data == null || data.length <= numberOutliers) throw new IllegalArgumentException("Not enough data to remove outliers.");

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
}
