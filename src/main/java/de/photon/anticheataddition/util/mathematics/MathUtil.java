package de.photon.anticheataddition.util.mathematics;

import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.util.Vector;

@UtilityClass
public final class MathUtil
{
    /**
     * Simple method to calculate the absolute offset of two numbers.
     *
     * @return the absolute offset, always positive or 0 if the numbers are equal.
     */
    public static int absDiff(final int a, final int b)
    {
        return a > b ? (a - b) : (b - a);
    }


    /**
     * Simple method to calculate the absolute offset of two numbers.
     *
     * @return the absolute offset, always positive or 0 if the numbers are equal.
     */
    public static long absDiff(final long a, final long b)
    {
        return a > b ? (a - b) : (b - a);
    }

    /**
     * Simple method to calculate the absolute offset of two numbers.
     *
     * @return the absolute offset, always positive or 0 if the numbers are equal.
     */
    public static double absDiff(final double a, final double b)
    {
        return a > b ? (a - b) : (b - a);
    }

    /**
     * Shortcut for number >= min && number <= max
     */
    public static boolean inRange(final double min, final double max, final double number)
    {
        return number >= min && number <= max;
    }

    /**
     * Calculates the sum of the elements from 0 to n.
     *
     * @param n the maximum number to sum to (0, 1, 2, 3, 4, ..., n)
     */
    public static int gaussianSumFormulaTo(final int n)
    {
        return (n * (n + 1)) >> 1;
    }

    /**
     * Uses the standard {@link Math#sqrt(double)} call to calculate the hypot of two numbers.
     * Make sure that the absolute value of both numbers are sufficiently small (smaller than 100,000) to avoid overflows.
     */
    public static double fastHypot(final double a, final double b)
    {
        return Math.sqrt(a * a + b * b);
    }

    /**
     * Fast squaring for streams.
     */
    public static int square(final int n)
    {
        return n * n;
    }

    /**
     * Fast squaring for streams.
     */
    public static double square(final double d)
    {
        return d * d;
    }

    /**
     * Returns the sum of all parameters squared.
     */
    public static int squareSum(int... ns)
    {
        int squareSum = 0;
        for (int n : ns) squareSum += n * n;
        return squareSum;
    }

    /**
     * Returns the sum of all parameters squared.
     */
    public static double squareSum(double... ns)
    {
        double squareSum = 0;
        for (double n : ns) squareSum += n * n;
        return squareSum;
    }

    /**
     * Computes the absolute shortest angular distance between two angles in the range [-180, 180].
     * This applies for example to the Minecraft yaw angles.
     *
     * @param yaw1 the first angle in degrees
     * @param yaw2 the second angle in degrees
     *
     * @return the absolute shortest angular distance in degrees
     */
    public static double yawDistance(double yaw1, double yaw2)
    {
        double diff = yaw1 - yaw2;
        return Math.abs(normalizeYaw(diff));
    }

    /**
     * Adds two yaw angles and normalizes the result to the range [-180, 180].
     *
     * @param yaw1 the first angle in degrees (Minecraft yaw, typically in [-180, 180])
     * @param yaw2 the second angle in degrees (could be any real number)
     *
     * @return the sum normalized into [-180, 180]
     */
    public static double yawAdd(double yaw1, double yaw2)
    {
        double sum = yaw1 + yaw2;
        return normalizeYaw(sum);
    }

    /**
     * Normalizes the yaw to the range [-180, 180].
     *
     * @param yaw the yaw angle in degrees
     *
     * @return the normalized yaw angle in degrees
     */
    public static double normalizeYaw(double yaw)
    {
        return ((yaw + 180) % 360 + 360) % 360 - 180;
    }

    /**
     * Generates the direction - vector from yaw and pitch, basically a copy of {@link Location#getDirection()}
     */
    @SuppressWarnings("RedundantCast")
    public static Vector getDirection(final float yaw, final float pitch)
    {
        final double yawRadians = Math.toRadians((double) yaw);
        final double pitchRadians = Math.toRadians((double) pitch);

        final var vector = new Vector();

        vector.setY(-Math.sin(pitchRadians));

        final double xz = Math.cos(pitchRadians);

        vector.setX(-xz * Math.sin(yawRadians));
        vector.setZ(xz * Math.cos(yawRadians));

        return vector;
    }

    /**
     * Calculates the angle between two rotations using {@link Vector}s.
     *
     * @return The angle between the two rotations in degrees.
     */
    public static float getAngleBetweenRotations(final float firstYaw, final float firstPitch, final float secondYaw, final float secondPitch)
    {
        final Vector first = getDirection(firstYaw, firstPitch);
        final Vector second = getDirection(secondYaw, secondPitch);

        return (float) Math.toDegrees(first.angle(second));
    }
}
