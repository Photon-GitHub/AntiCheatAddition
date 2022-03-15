package de.photon.anticheataddition.util.mathematics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

class MathUtilTest
{
    private final Random random = new Random();

    @Test
    void absDiffTest()
    {
        Assertions.assertEquals(0, MathUtil.absDiff(0, 0));
        Assertions.assertEquals(3, MathUtil.absDiff(2, 5));
        Assertions.assertEquals(3, MathUtil.absDiff(5, 2));
        Assertions.assertEquals(7, MathUtil.absDiff(-2, 5));
        Assertions.assertEquals(7, MathUtil.absDiff(-5, 2));
        random.ints(10).forEach(i -> Assertions.assertEquals(0, MathUtil.absDiff(i, i)));
        random.ints(10).forEach(i -> Assertions.assertEquals(Math.abs(i), MathUtil.absDiff(0, i)));
        random.ints(10).forEach(i -> Assertions.assertEquals(Math.abs(i), MathUtil.absDiff(i, 0)));
        random.ints(10, -10000, 10000).forEach(i -> Assertions.assertEquals(2 * Math.abs(i), MathUtil.absDiff(-i, i)));
    }

    @Test
    void testGaussianSum()
    {
        int sum = 0;
        for (int i = 0; i < 100; ++i) {
            sum += i;
            Assertions.assertEquals(sum, MathUtil.gaussianSumFormulaTo(i));
        }
    }

    @Test
    void testPow()
    {
        for (int base = 0; base < 15; base++) {
            for (int pow = 0; pow < 7; pow++) {
                Assertions.assertEquals((int) Math.pow(base, pow), MathUtil.pow(base, pow));
            }
        }
    }
}
