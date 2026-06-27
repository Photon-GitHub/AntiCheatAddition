package de.photon.anticheataddition.util.mathematics;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.stream.DoubleStream;

import static org.junit.jupiter.api.Assertions.*;

class KolmogorovSmirnovTest {
    private static final double DELTA = 1e-12;

    @Test
    void normalizeDoublesScalesAndSorts()
    {
        final double[] data = {2.0, 4.0, 6.0, 8.0};
        final double[] expected = {0.0, 1.0 / 3, 2.0 / 3, 1.0};

        assertArrayEquals(expected, KolmogorovSmirnov.normalize(data), DELTA);
    }

    @Test
    void normalizeDoublesAllowsEmptyInput()
    {
        assertArrayEquals(new double[0], KolmogorovSmirnov.normalize(new double[0]));
    }

    @Test
    void normalizeDoublesMapsConstantInputToZeroes()
    {
        final double[] expected = {0.0, 0.0, 0.0};
        final double[] data = {5.0, 5.0, 5.0};

        assertArrayEquals(expected, KolmogorovSmirnov.normalize(data), 0.0);
    }

    @Test
    void normalizeDoublesRejectsNonFiniteValues()
    {
        assertThrows(IllegalArgumentException.class, () -> KolmogorovSmirnov.normalize(new double[]{1.0, Double.NaN}));
        assertThrows(IllegalArgumentException.class, () -> KolmogorovSmirnov.normalize(new double[]{1.0, Double.POSITIVE_INFINITY}));
    }

    @Test
    void normalizeDoublesHandlesOverflowingRange()
    {
        final double[] data = {-Double.MAX_VALUE, 0.0, Double.MAX_VALUE};
        final double[] expected = {0.0, 0.5, 1.0};

        assertArrayEquals(expected, KolmogorovSmirnov.normalize(data), DELTA);
    }

    @Test
    void normalizeLongsScalesAndSorts()
    {
        final long[] data = {2L, 4L, 6L, 8L};
        final double[] expected = {0.0, 1.0 / 3, 2.0 / 3, 1.0};

        assertArrayEquals(expected, KolmogorovSmirnov.normalize(data), DELTA);
    }

    @Test
    void normalizeLongsPreservesAdjacentLargeValues()
    {
        final long[] data = {Long.MAX_VALUE, Long.MAX_VALUE - 1};
        final double[] expected = {0.0, 1.0};

        assertArrayEquals(expected, KolmogorovSmirnov.normalize(data), 0.0);
    }

    @Test
    void normalizeLongsHandlesFullLongRange()
    {
        final long[] data = {Long.MIN_VALUE, 0L, Long.MAX_VALUE};
        final double[] normalized = KolmogorovSmirnov.normalize(data);

        assertEquals(0.0, normalized[0], 0.0);
        assertEquals(0.5, normalized[1], DELTA);
        assertEquals(1.0, normalized[2], 0.0);
    }

    @Test
    void uniformTestComputesStatisticAndApproximatePValue()
    {
        final double[] sample = {2.0, 4.0, 6.0, 8.0};

        KolmogorovSmirnov.KsResult result = KolmogorovSmirnov.uniformTest(sample);

        assertEquals(0.25, result.dStatistic(), DELTA);
        assertEquals(0.9289547774020108, result.pValue(), DELTA);
        assertTrue(result.significanceTest(0.05));
    }

    @Test
    void seededUniformSampleIsNotRejected()
    {
        final Random rng = new Random(0);

        final double[] samples = new double[1000];
        for (int i = 0; i < samples.length; i++) samples[i] = rng.nextDouble();

        KolmogorovSmirnov.KsResult result = KolmogorovSmirnov.uniformTest(samples);
        assertTrue(result.significanceTest(0.05), () -> "p = " + result.pValue() + " should be >= 0.05");
    }

    @Test
    void seededNormalSampleIsRejected()
    {
        final Random rng = new Random(0);
        final double[] samples = new double[1000];
        for (int i = 0; i < samples.length; i++) samples[i] = rng.nextGaussian(0.5, 0.2);

        KolmogorovSmirnov.KsResult result = KolmogorovSmirnov.uniformTest(samples);
        assertFalse(result.significanceTest(0.05), () -> "p = " + result.pValue() + " should be < 0.05");
    }

    @Test
    void seededExponentialSampleIsRejected()
    {
        final Random rng = new Random(0);
        double[] sample = rng.doubles(1_000)
                             .map(u -> -Math.log1p(-u))
                             .toArray();

        KolmogorovSmirnov.KsResult result = KolmogorovSmirnov.uniformTest(sample);
        assertFalse(result.significanceTest(0.05), () -> "p = " + result.pValue() + " should be < 0.05");
    }

    @Test
    void constantSampleIsRejected()
    {
        final double[] sample = new double[11];

        KolmogorovSmirnov.KsResult result = KolmogorovSmirnov.uniformTest(sample);

        assertEquals(1.0, result.dStatistic(), DELTA);
        assertTrue(result.pValue() < 1e-10, () -> "p = " + result.pValue() + " should be tiny");
        assertFalse(result.significanceTest(0.99));
    }

    @Test
    void uniformTestRequiresAtLeastTwoValues()
    {
        IllegalArgumentException empty = assertThrows(IllegalArgumentException.class,
                                                      () -> KolmogorovSmirnov.uniformTest(new double[0]));
        IllegalArgumentException single = assertThrows(IllegalArgumentException.class,
                                                       () -> KolmogorovSmirnov.uniformTest(new double[]{1.0}));

        assertTrue(empty.getMessage().contains("at least 2"));
        assertTrue(single.getMessage().contains("at least 2"));
    }

    @Test
    void significanceTestRejectsInvalidAlpha()
    {
        KolmogorovSmirnov.KsResult result = new KolmogorovSmirnov.KsResult(0.0, 1.0);

        assertThrows(IllegalArgumentException.class, () -> result.significanceTest(0.0));
        assertThrows(IllegalArgumentException.class, () -> result.significanceTest(1.0));
    }
}
