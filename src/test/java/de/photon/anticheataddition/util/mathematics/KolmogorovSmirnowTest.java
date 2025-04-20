package de.photon.anticheataddition.util.mathematics;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

class KolmogorovSmirnovTest
{

    /* ─────────────────── normalisation helpers ─────────────────── */

    @Test
    void normalizeDoubles_basic()
    {
        double[] data = {2.0, 4.0, 6.0, 8.0};
        double[] expected = {0.0, 1.0 / 3, 2.0 / 3, 1.0};

        assertArrayEquals(expected,
                          KolmogorovSmirnov.normalize(data),
                          1e-12);
    }

    @Test
    void normalizeDoubles_empty()
    {
        assertArrayEquals(new double[0],
                          KolmogorovSmirnov.normalize(new double[0]));
    }

    @Test
    void normalizeDoubles_constant()
    {
        double[] expected = {0.0, 0.0, 0.0};
        double[] data = {5.0, 5.0, 5.0};

        assertArrayEquals(expected,
                          KolmogorovSmirnov.normalize(data),
                          0.0);
    }

    @Test
    void normalizeLongs_basic()
    {
        long[] data = {2L, 4L, 6L, 8L};
        double[] expected = {0.0, 1.0 / 3, 2.0 / 3, 1.0};

        assertArrayEquals(expected,
                          KolmogorovSmirnov.normalize(data),
                          1e-12);
    }

    /* ───────────────────── uniform vs non‑uniform ───────────────────── */

    @Test
    void uniformSample_isNotRejected()
    {
        Random rng = new Random(0);
        double[] sample = rng.doubles(1_000).toArray();        // U(0,1)

        KolmogorovSmirnov.KsResult r = KolmogorovSmirnov.uniformTest(sample);
        System.out.println(r);
        assertTrue(r.significanceTest(0.05), () -> "p = " + r.pValue() + " should be ≥ 0.05");
    }

    @Test
    void normalSample_isRejected()
    {
        Random rng = new Random(0);
        double[] sample = rng.doubles(1_000)
                             .map(x -> rng.nextGaussian() * 0.2 + 0.5) // N(0.5, 0.2²)
                             .toArray();

        KolmogorovSmirnov.KsResult r = KolmogorovSmirnov.uniformTest(sample);
        System.out.println(r);

        assertFalse(r.significanceTest(0.05),
                    () -> "p = " + r.pValue() + " should be < 0.05");
    }

    @Test
    void exponentialSample_isRejected()
    {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double[] sample = rng.doubles(1_000)
                             .map(u -> -Math.log1p(-u))          // Exp(λ=1)
                             .toArray();

        KolmogorovSmirnov.KsResult r = KolmogorovSmirnov.uniformTest(sample);
        System.out.println(r);

        assertFalse(r.significanceTest(0.05),
                    () -> "p = " + r.pValue() + " should be < 0.05");
    }

    /* ───────────────────────── edge cases ───────────────────────── */


    @Test
    void constantSample_alwaysRejected() {
        double[] sample = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};

        var res = KolmogorovSmirnov.uniformTest(sample);

        assertTrue(res.pValue() < 1e-10,() -> "p = " + res.pValue() + " should be tiny");
        assertFalse(res.significanceTest(0.99));   // still rejected even at 1% α
    }


    @Test
    void emptySample_throws()
    {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> KolmogorovSmirnov.uniformTest(new double[0]));

        assertTrue(ex.getMessage().contains("at least 2"));
    }

    @Test
    void significanceTest_invalidAlpha_throws()
    {
        KolmogorovSmirnov.KsResult r = new KolmogorovSmirnov.KsResult(0.0, 1.0);
        System.out.println(r);

        assertThrows(IllegalArgumentException.class, () -> r.significanceTest(0.0));
        assertThrows(IllegalArgumentException.class, () -> r.significanceTest(1.0));
    }
}