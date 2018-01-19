package de.photon.AACAdditionPro.util.mathematics;


import org.bukkit.Location;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class MathUtils
{
    /**
     * Calculates the distance between two vectors in n-dimensional space.
     */
    public static double vectorDistance(double[] firstVector, double[] secondVector)
    {
        if (Objects.requireNonNull(firstVector, "First vector is null").length != Objects.requireNonNull(secondVector, "Second vector is null").length)
        {
            throw new IllegalArgumentException("Vectors have not the same amount of dimensions.");
        }

        // float is sufficient here as the data will not be based on very accurate numbers.
        double sum = 0;
        for (int i = 0; i < firstVector.length; i++)
        {
            sum += Math.pow(secondVector[i] - firstVector[i], 2);
        }

        return Math.sqrt(sum);
    }

    /**
     * Simple method to know if a number is close to another number
     *
     * @param a     The first number
     * @param b     The second number
     * @param range The maximum search range
     *
     * @return true if the numbers are in range of one another else false
     */
    public static boolean roughlyEquals(final double a, final double b, final double range)
    {
        return offset(a, b) <= range;
    }

    /**
     * Simple method to calculate the absolute offset of two numbers.
     *
     * @return the absolute offset, always positive or 0 if the numbers are equal.
     */
    public static double offset(final double a, final double b)
    {
        return a > b ? (a - b) : (b - a);
    }

    /**
     * Generates a new random integer.
     *
     * @param min            the result will at least be this parameter
     * @param randomBoundary the result will at most be min + randomBoundary
     *
     * @return the resulting random integer
     */
    public static int randomBoundaryInt(int min, int randomBoundary)
    {
        return min + ThreadLocalRandom.current().nextInt(randomBoundary);
    }

    /**
     * Generates a new random double.
     *
     * @param min            the result will at least be this parameter
     * @param randomBoundary the result will at most be min + randomBoundary
     *
     * @return the resulting random double
     */
    public static double randomBoundaryDouble(double min, double randomBoundary)
    {
        return min + ThreadLocalRandom.current().nextDouble(randomBoundary);
    }

    /**
     * Simple method to know if a {@link Location} is close to another {@link Location}
     *
     * @param firstLocation  the first {@link Location}
     * @param secondLocation the second {@link Location}
     * @param x              how far should the {@link Location}s at most be apart on the x-Axis
     * @param y              how far should the {@link Location}s at most be apart on the y-Axis
     * @param z              how far should the {@link Location}s at most be apart on the z-Axis
     *
     * @return true if the {@link Location} are in range, false if not
     */
    public static boolean areLocationsInRange(final Location firstLocation, final Location secondLocation, final double x, final double y, final double z)
    {
        return firstLocation.getWorld().getName().equals(secondLocation.getWorld().getName()) &&
               roughlyEquals(firstLocation.getX(), secondLocation.getX(), x) &&
               roughlyEquals(firstLocation.getY(), secondLocation.getY(), y) &&
               roughlyEquals(firstLocation.getZ(), secondLocation.getZ(), z);
    }

    /**
     * Simple method to know if a {@link Location} is close to another {@link Location}
     *
     * @param firstLocation   the first {@link Location}
     * @param secondLocation  the second {@link Location}
     * @param squaredDistance the squared distance that must be at most between the two {@link Location}s to make this {@link java.lang.reflect.Method} return true.
     *
     * @return true if the {@link Location} are in range, false if not
     */
    public static boolean areLocationsInRange(final Location firstLocation, final Location secondLocation, final double squaredDistance)
    {
        return firstLocation.getWorld().getName().equals(secondLocation.getWorld().getName()) &&
               firstLocation.distanceSquared(secondLocation) <= squaredDistance;
    }

    /**
     * Fixes the rotation for the {@link de.photon.AACAdditionPro.util.entities.ClientsideEntity}s
     */
    public static byte getFixRotation(final float yawpitch)
    {
        return (byte) ((int) (yawpitch * 256.0F / 360.0F));
    }
}
