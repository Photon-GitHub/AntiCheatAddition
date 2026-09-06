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
     * @return the absolute shortest angular distance in degrees
     */
    public static double yawDistance(double yaw1, double yaw2)
    {
        return Math.abs(signedYawDelta(yaw1, yaw2));
    }

    /**
     * Adds two yaw angles and normalizes the result to the range [-180, 180].
     *
     * @param yaw1 the first angle in degrees (Minecraft yaw, typically in [-180, 180])
     * @param yaw2 the second angle in degrees (could be any real number)
     * @return the sum normalized into [-180, 180]
     */
    public static double yawAdd(double yaw1, double yaw2)
    {
        double sum = normalizeYaw(yaw1) + normalizeYaw(yaw2);
        return normalizeYaw(sum);
    }

    /**
     * Generates the direction - vector from yaw and pitch, basically a copy of {@link Location#getDirection()}
     */
    @SuppressWarnings("RedundantCast")
    public static Vector getDirection(final float yaw, final float pitch)
    {
        final double yawRadians = Math.toRadians(normalizeYaw(yaw));
        final double pitchRadians = Math.toRadians((double) pitch);
        final double pitchCosine = Math.cos(pitchRadians);

        return new Vector(
                -pitchCosine * Math.sin(yawRadians),
                -Math.sin(pitchRadians),
                pitchCosine * Math.cos(yawRadians)
        );
    }

    /**
     * Canonical yaw in [-180, 180), or NaN for non-finite input. Reduces before adding to avoid precision loss.
     */
    public static double normalizeYaw(final double yaw)
    {
        if (!Double.isFinite(yaw)) return Double.NaN;
        double normalized = yaw % 360D;
        if (normalized >= 180D) normalized -= 360D;
        else if (normalized < -180D) normalized += 360D;
        return normalized == -0D ? 0D : normalized;
    }

    /**
     * Shortest signed yaw delta in [-180, 180], reducing each input before subtraction to avoid overflow.
     */
    public static double signedYawDelta(final double currentYaw, final double previousYaw)
    {
        final double normalizedCurrent = normalizeYaw(currentYaw);
        final double normalizedPrevious = normalizeYaw(previousYaw);
        if (!Double.isFinite(normalizedCurrent) || !Double.isFinite(normalizedPrevious)) return Double.NaN;

        double delta = normalizedCurrent - normalizedPrevious;
        if (delta > 180D) delta -= 360D;
        else if (delta < -180D) delta += 360D;
        return delta;
    }

    public static float getAngleBetweenRotations(final float firstYaw, final float firstPitch, final float secondYaw, final float secondPitch)
    {
        return (float) getAngleBetweenRotations((double) firstYaw, firstPitch, secondYaw, secondPitch);
    }

    /**
     * Great-circle angular distance in degrees, with yaw normalized before trigonometry.
     */
    public static double getAngleBetweenRotations(final double firstYaw,
                                                  final double firstPitch,
                                                  final double secondYaw,
                                                  final double secondPitch)
    {
        final double yawDelta = signedYawDelta(firstYaw, secondYaw);
        if (yawDelta == 0D && firstPitch == secondPitch) return 0D;

        final double firstPitchRadians = Math.toRadians(firstPitch);
        final double secondPitchRadians = Math.toRadians(secondPitch);

        final double firstCosPitch = Math.cos(firstPitchRadians);
        final double secondCosPitch = Math.cos(secondPitchRadians);
        final double dot = Math.clamp(firstCosPitch * secondCosPitch * Math.cos(Math.toRadians(yawDelta)) + Math.sin(firstPitchRadians) * Math.sin(secondPitchRadians), -1D, 1D);
        return Math.toDegrees(Math.acos(dot));
    }

}
