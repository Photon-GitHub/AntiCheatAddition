package de.photon.AACAdditionPro.util.mathematics;

public final class MathUtils
{
    private MathUtils() {}

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
     * Shortcut for number >= min && number <= max
     */
    public static boolean inRange(final int min, final int max, final int number)
    {
        return number >= min && number <= max;
    }

    /**
     * Shortcut for number >= min && number <= max
     */
    public static boolean inRange(final double min, final double max, final double number)
    {
        return number >= min && number <= max;
    }
}
