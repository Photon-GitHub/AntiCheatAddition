package de.photon.anticheataddition.util.mathematics;

import lombok.experimental.UtilityClass;

import java.util.PriorityQueue;

@UtilityClass
public final class DataUtil
{
    public static int sum(int... data)
    {
        int sum = 0;
        for (int datum : data) sum += datum;
        return sum;
    }

    public static long sum(long... data)
    {
        long sum = 0;
        for (long datum : data) sum += datum;
        return sum;
    }

    public static double sum(double... data)
    {
        double sum = 0;
        for (double datum : data) sum += datum;
        return sum;
    }

    public static double average(int... data)
    {
        return sum(data) / (double) data.length;
    }

    public static double average(long... data)
    {
        return sum(data) / (double) data.length;
    }

    public static double average(double... data)
    {
        return sum(data) / data.length;
    }

    /**
     * Shortcut for calculating the squared error of a certain value.
     */
    public static double variance(final double reference, final double value)
    {
        final double error = value - reference;
        return error * error;
    }

    /**
     * Calculates the summed square error from the reference value.
     */
    public static double variance(double reference, int... data)
    {
        double sum = 0;
        for (int datum : data) sum += variance(reference, datum);
        return sum;
    }

    /**
     * Calculates the summed square error from the reference value.
     */
    public static double variance(double reference, long... data)
    {
        double sum = 0;
        for (long datum : data) sum += variance(reference, datum);
        return sum;
    }

    /**
     * Calculates the summed square error from the reference value.
     */
    public static double variance(double reference, double... data)
    {
        double sum = 0;
        for (double datum : data) sum += variance(reference, datum);
        return sum;
    }

    /**
     * Removes the numberOutliers farthest elements from the mean.
     */
    public static int[] removeOutliers(int numberOutliers, int... data)
    {
        if (data == null || data.length <= numberOutliers) throw new IllegalArgumentException("Not enough data to remove outliers.");

        // Calculate the mean using predefined methods
        final double mean = average(data);

        // Priority queue to store the farthest elements from the mean
        final PriorityQueue<Integer> outliers = new PriorityQueue<>((a, b) -> Double.compare(Math.abs(b - mean), Math.abs(a - mean)));

        // Add all data points to the priority queue
        for (int value : data) {
            outliers.add(value);
        }

        // Remove the specified number of outliers
        for (int i = 0; i < numberOutliers; i++) {
            outliers.poll();
        }
        // Convert the remaining elements in the priority queue to an array
        return outliers.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Removes the numberOutliers farthest elements from the mean.
     */
    public static long[] removeOutliers(int numberOutliers, long... data)
    {
        if (data == null || data.length <= numberOutliers) throw new IllegalArgumentException("Not enough data to remove outliers.");

        // Calculate the mean using predefined methods
        final double mean = average(data);

        // Priority queue to store the farthest elements from the mean
        final PriorityQueue<Long> outliers = new PriorityQueue<>((a, b) -> Double.compare(Math.abs(b - mean), Math.abs(a - mean)));

        // Add all data points to the priority queue
        for (long value : data) {
            outliers.add(value);
        }

        // Remove the specified number of outliers
        for (int i = 0; i < numberOutliers; i++) {
            outliers.poll();
        }

        // Convert the remaining elements in the priority queue to an array
        return outliers.stream().mapToLong(Long::longValue).toArray();
    }

    /**
     * Removes the numberOutliers farthest elements from the mean.
     */
    public static double[] removeOutliers(int numberOutliers, double... data)
    {
        if (data == null || data.length <= numberOutliers) throw new IllegalArgumentException("Not enough data to remove outliers.");

        // Calculate the mean using predefined methods
        final double mean = average(data);

        // Priority queue to store the farthest elements from the mean
        final PriorityQueue<Double> outliers = new PriorityQueue<>((a, b) -> Double.compare(Math.abs(b - mean), Math.abs(a - mean)));

        // Add all data points to the priority queue
        for (double value : data) {
            outliers.add(value);
        }

        // Remove the specified number of outliers
        for (int i = 0; i < numberOutliers; i++) {
            outliers.poll();
        }

        // Convert the remaining elements in the priority queue to an array
        return outliers.stream().mapToDouble(Double::doubleValue).toArray();
    }
}
