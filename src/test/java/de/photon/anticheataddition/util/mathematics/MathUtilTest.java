package de.photon.anticheataddition.util.mathematics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MathUtilTest
{
    public static final double DELTA = 0.0001;
    private final Random random = new Random();

    @Test
    void absDiffTest()
    {
        assertEquals(0, MathUtil.absDiff(0, 0));
        assertEquals(3, MathUtil.absDiff(2, 5));
        assertEquals(3, MathUtil.absDiff(5, 2));
        assertEquals(7, MathUtil.absDiff(-2, 5));
        assertEquals(7, MathUtil.absDiff(-5, 2));
        random.ints(10).forEach(i -> assertEquals(0, MathUtil.absDiff(i, i)));
        random.ints(10).forEach(i -> assertEquals(Math.abs(i), MathUtil.absDiff(0, i)));
        random.ints(10).forEach(i -> assertEquals(Math.abs(i), MathUtil.absDiff(i, 0)));
        random.ints(10, -10000, 10000).forEach(i -> assertEquals(2 * Math.abs(i), MathUtil.absDiff(-i, i)));
    }

    @Test
    void testGaussianSum()
    {
        int sum = 0;
        for (int i = 0; i < 100; ++i) {
            sum += i;
            assertEquals(sum, MathUtil.gaussianSumFormulaTo(i));
        }
    }

    @Test
    void testFastHypotCalculatesCorrectly()
    {
        assertEquals(5, MathUtil.fastHypot(3, 4), DELTA);
        assertEquals(Math.hypot(2, 3), MathUtil.fastHypot(2, 3), DELTA);
        assertEquals(0, MathUtil.fastHypot(0, 0), DELTA);
    }

    @Test
    void testSquareIntCalculatesCorrectly()
    {
        assertEquals(16, MathUtil.square(4));
        assertEquals(1, MathUtil.square(-1));
        assertEquals(0, MathUtil.square(0));
    }

    @Test
    void testSquareDoubleCalculatesCorrectly()
    {
        assertEquals(16.0, MathUtil.square(4.0), DELTA);
        assertEquals(1.0, MathUtil.square(-1.0), DELTA);
        assertEquals(0.0, MathUtil.square(0.0), DELTA);
    }

    @Test
    void testSquareSumInt()
    {
        assertEquals(30, MathUtil.squareSum(1, 2, 3, 4));
        assertEquals(30, MathUtil.squareSum(-1, -2, 3, 4));
        assertEquals(30, MathUtil.squareSum(3, 2, 4, 1));
        assertEquals(0, MathUtil.squareSum(0, 0, 0, 0));
    }

    @Test
    void testSquareSumDouble()
    {
        assertEquals(30, MathUtil.squareSum(1D, 2D, 3D, 4D), DELTA);
        assertEquals(30, MathUtil.squareSum(-1D, -2D, 3D, 4D), DELTA);
        assertEquals(30, MathUtil.squareSum(3D, 2D, 4D, 1D), DELTA);
        assertEquals(0, MathUtil.squareSum(0D, 0D, 0D, 0D), DELTA);
    }

    @Test
    void testShortestAngleDistance()
    {
        // Basic cases
        assertEquals(20.0, MathUtil.yawDistance(170, -170), DELTA);
        assertEquals(180.0, MathUtil.yawDistance(90, -90), DELTA);
        assertEquals(0.0, MathUtil.yawDistance(-180, 180), DELTA);
        assertEquals(90.0, MathUtil.yawDistance(-45, 45), DELTA);

        // Same angle
        assertEquals(0.0, MathUtil.yawDistance(0, 0), DELTA);
        assertEquals(0.0, MathUtil.yawDistance(45, 45), DELTA);
        assertEquals(0.0, MathUtil.yawDistance(-90, -90), DELTA);

        // Crossing the -180/180 boundary
        assertEquals(10.0, MathUtil.yawDistance(-175, 175), DELTA);
        assertEquals(10.0, MathUtil.yawDistance(175, -175), DELTA);

        // Symmetry of angles
        assertEquals(20.0, MathUtil.yawDistance(-170, 170), DELTA);
        assertEquals(20.0, MathUtil.yawDistance(170, -170), DELTA);

        // Large difference within range
        assertEquals(180.0, MathUtil.yawDistance(180, 0), DELTA);
        assertEquals(180.0, MathUtil.yawDistance(-180, 0), DELTA);

        // Edge cases
        assertEquals(180.0, MathUtil.yawDistance(0, 180), DELTA);
        assertEquals(180.0, MathUtil.yawDistance(0, -180), DELTA);

        // Small differences
        assertEquals(1.0, MathUtil.yawDistance(179, -180), DELTA);
        assertEquals(1.0, MathUtil.yawDistance(-180, 179), DELTA);
        assertEquals(0.5, MathUtil.yawDistance(179.5, -180), DELTA);
        assertEquals(0.5, MathUtil.yawDistance(-180, 179.5), DELTA);

        // Opposite angles
        assertEquals(180.0, MathUtil.yawDistance(-90, 90), DELTA);
        assertEquals(180.0, MathUtil.yawDistance(90, -90), DELTA);
    }
}
