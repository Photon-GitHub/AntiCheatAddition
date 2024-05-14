package de.photon.anticheataddition.util.mathematics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class KolmogorovSmirnowTest
{

    @Test
    void testNormalizeDataWithDoubles()
    {
        double[] data = {2.0, 4.0, 6.0, 8.0};
        double[] expected = {0.0, 0.3333333333333333, 0.6666666666666666, 1.0};

        double[] result = KolmogorovSmirnow.normalizeData(data);

        assertArrayEquals(expected, result, 1e-9);
    }

    @Test
    void testNormalizeDataWithDoublesEmptyArray()
    {
        double[] data = {};
        double[] expected = {};

        double[] result = KolmogorovSmirnow.normalizeData(data);

        assertArrayEquals(expected, result, 1e-9);
    }

    @Test
    void testNormalizeDataWithDoublesSameValues()
    {
        double[] data = {5.0, 5.0, 5.0};
        double[] expected = {0.0, 0.0, 0.0};

        double[] result = KolmogorovSmirnow.normalizeData(data);

        assertArrayEquals(expected, result, 1e-9);
    }

    @Test
    void testNormalizeDataWithLongs()
    {
        long[] data = {2L, 4L, 6L, 8L};
        double[] expected = {0.0, 0.3333333333333333, 0.6666666666666666, 1.0};

        double[] result = KolmogorovSmirnow.normalizeData(data);

        assertArrayEquals(expected, result, 1e-9);
    }

    @Test
    void testNormalizeDataWithLongsEmptyArray()
    {
        long[] data = {};
        double[] expected = {};

        double[] result = KolmogorovSmirnow.normalizeData(data);

        assertArrayEquals(expected, result, 1e-9);
    }

    @Test
    void testNormalizeDataWithLongsSameValues()
    {
        long[] data = {5L, 5L, 5L};
        double[] expected = {0.0, 0.0, 0.0};

        double[] result = KolmogorovSmirnow.normalizeData(data);

        assertArrayEquals(expected, result, 1e-9);
    }

    @Test
    void testKolmogorovSmirnowUniformTestEmptyArray()
    {
        double[] sample = {};
        double expected = 1.0;

        double result = KolmogorovSmirnow.kSTestForUniformDistribution(sample);

        assertEquals(expected, result, 1e-9);
    }

    @Test
    void testKolmogorovSmirnowUniformTestNullArray()
    {
        double[] sample = null;
        double expected = 1.0;

        double result = KolmogorovSmirnow.kSTestForUniformDistribution(sample);

        assertEquals(expected, result, 1e-9);
    }

    @Test
    void testKolmogorovSmirnowUniformDistribution()
    {
        double[] sample = KolmogorovSmirnow.normalizeData(generateUniformDistribution(1000));
        final double d_max = KolmogorovSmirnow.kSTestForUniformDistribution(sample);

        Assertions.assertTrue(d_max < 0.1, "D statistic should be close to zero 0 for a uniform distribution");
    }

    @Test
    void testKolmogorovSmirnowNormalDistribution()
    {
        double[] sample = KolmogorovSmirnow.normalizeData(generateNormalDistribution(1000, 5, 10));
        double d_max = KolmogorovSmirnow.kSTestForUniformDistribution(sample);

        // The D statistic should be significantly higher than 0 for a normal distribution
        Assertions.assertTrue(d_max > 0.21, "D statistic should be significantly higher than 0 for a normal distribution");
    }

    @Test
    void testKolmogorovSmirnowExponentialDistribution()
    {
        double[] sample = generateExponentialDistribution(1000);
        double d_max = KolmogorovSmirnow.kSTestForUniformDistribution(sample);

        // The D statistic should be significantly higher than 0 for a Poisson distribution
        Assertions.assertTrue(d_max > 0.21, "D statistic should be significantly higher than 0 for a Poisson distribution");
    }

    // Utility methods for generating distributions
    private double[] generateUniformDistribution(int size)
    {
        Random random = new Random();
        return random.doubles(size).toArray();
    }

    private double[] generateNormalDistribution(int size, double mean, double stddev)
    {
        Random random = new Random();
        final double[] data = new double[size];
        for (int i = 0; i < size; i++) data[i] = random.nextGaussian(mean, stddev);
        return data;
    }

    private double[] generateExponentialDistribution(int size)
    {
        Random random = new Random();
        final double[] data = new double[size];
        for (int i = 0; i < size; i++) data[i] = random.nextExponential();
        return data;
    }
}
