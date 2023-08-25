package de.photon.anticheataddition.util.mathematics;

import lombok.experimental.UtilityClass;

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

}
