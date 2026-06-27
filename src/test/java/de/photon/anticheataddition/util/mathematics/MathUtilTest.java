package de.photon.anticheataddition.util.mathematics;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void absDiffLongTest()
    {
        assertEquals(0L, MathUtil.absDiff(0L, 0L));
        assertEquals(3L, MathUtil.absDiff(2L, 5L));
        assertEquals(3L, MathUtil.absDiff(5L, 2L));
        assertEquals(7L, MathUtil.absDiff(-2L, 5L));
        assertEquals(7L, MathUtil.absDiff(-5L, 2L));
        assertEquals(2_000_000_000L, MathUtil.absDiff(-1_000_000_000L, 1_000_000_000L));
    }

    @Test
    void absDiffDoubleTest()
    {
        assertEquals(0.0, MathUtil.absDiff(0.0, 0.0), DELTA);
        assertEquals(3.0, MathUtil.absDiff(2.0, 5.0), DELTA);
        assertEquals(3.0, MathUtil.absDiff(5.0, 2.0), DELTA);
        assertEquals(7.5, MathUtil.absDiff(-2.5, 5.0), DELTA);
        assertEquals(7.5, MathUtil.absDiff(-5.0, 2.5), DELTA);
    }

    @Test
    void testInRange()
    {
        assertTrue(MathUtil.inRange(-90, 90, -90));
        assertTrue(MathUtil.inRange(-90, 90, 0));
        assertTrue(MathUtil.inRange(-90, 90, 90));
        assertFalse(MathUtil.inRange(-90, 90, -90.1));
        assertFalse(MathUtil.inRange(-90, 90, 90.1));
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

    @Test
    void testYawAdd()
    {
        assertEquals(0.0, MathUtil.yawAdd(0, 0), DELTA);
        assertEquals(-170.0, MathUtil.yawAdd(170, 20), DELTA);
        assertEquals(170.0, MathUtil.yawAdd(-170, -20), DELTA);
        assertEquals(-180.0, MathUtil.yawAdd(90, 90), DELTA);
        assertEquals(45.0, MathUtil.yawAdd(765, 0), DELTA);
    }

    @Test
    void testNormalizeYaw()
    {
        assertEquals(0.0, MathUtil.normalizeYaw(0), DELTA);
        assertEquals(-180.0, MathUtil.normalizeYaw(180), DELTA);
        assertEquals(-180.0, MathUtil.normalizeYaw(-180), DELTA);
        assertEquals(-179.0, MathUtil.normalizeYaw(181), DELTA);
        assertEquals(179.0, MathUtil.normalizeYaw(-181), DELTA);
        assertEquals(0.0, MathUtil.normalizeYaw(360), DELTA);
        assertEquals(-180.0, MathUtil.normalizeYaw(540), DELTA);
        assertEquals(90.0, MathUtil.normalizeYaw(-630), DELTA);
    }

    @Test
    void testGetDirection()
    {
        assertVector(0.0, 0.0, 1.0, MathUtil.getDirection(0F, 0F));
        assertVector(-1.0, 0.0, 0.0, MathUtil.getDirection(90F, 0F));
        assertVector(1.0, 0.0, 0.0, MathUtil.getDirection(-90F, 0F));
        assertVector(0.0, -1.0, 0.0, MathUtil.getDirection(0F, 90F));
        assertVector(0.0, 1.0, 0.0, MathUtil.getDirection(0F, -90F));
    }

    @Test
    void testGetAngleBetweenRotations()
    {
        assertEquals(0.0F, MathUtil.getAngleBetweenRotations(0F, 0F, 0F, 0F), DELTA);
        assertEquals(90.0F, MathUtil.getAngleBetweenRotations(0F, 0F, 90F, 0F), DELTA);
        assertEquals(180.0F, MathUtil.getAngleBetweenRotations(0F, 0F, 180F, 0F), DELTA);
        assertEquals(90.0F, MathUtil.getAngleBetweenRotations(0F, 0F, 0F, 90F), DELTA);
        assertEquals(180.0F, MathUtil.getAngleBetweenRotations(0F, 90F, 0F, -90F), DELTA);
    }

    @Test
    void testGetAngleBetweenRotationsMatchesVectorAngle()
    {
        final float[] yaws = {-540F, -180F, -90F, 0F, 45F, 90F, 180F, 540F};
        final float[] pitches = {-89F, -45F, 0F, 45F, 89F};

        for (final float firstYaw : yaws) {
            for (final float firstPitch : pitches) {
                for (final float secondYaw : yaws) {
                    for (final float secondPitch : pitches) {
                        final Vector first = MathUtil.getDirection(firstYaw, firstPitch);
                        final Vector second = MathUtil.getDirection(secondYaw, secondPitch);
                        final double expected = Math.toDegrees(first.angle(second));
                        assertEquals(expected, MathUtil.getAngleBetweenRotations(firstYaw, firstPitch, secondYaw, secondPitch), DELTA);
                    }
                }
            }
        }
    }

    private static void assertVector(final double expectedX, final double expectedY, final double expectedZ, final Vector actual)
    {
        assertEquals(expectedX, actual.getX(), DELTA);
        assertEquals(expectedY, actual.getY(), DELTA);
        assertEquals(expectedZ, actual.getZ(), DELTA);
    }
}
