package de.photon.AACAdditionPro.util.random;

import java.util.concurrent.ThreadLocalRandom;

public final class RandomUtil
{
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
}
