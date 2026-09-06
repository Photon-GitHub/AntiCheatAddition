package de.photon.anticheataddition.util.mathematics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class TargetingMathUtilTest
{
    @Test
    void preservesLargeYawEquivalenceAndAvoidsOverflow()
    {
        final double canonical = MathUtil.normalizeYaw(Double.MAX_VALUE);
        assertEquals(0D, MathUtil.yawDistance(Double.MAX_VALUE, canonical));
        assertTrue(Double.isFinite(MathUtil.yawDistance(Double.MAX_VALUE, -Double.MAX_VALUE)));
        assertEquals(MathUtil.normalizeYaw(canonical * 2D), MathUtil.yawAdd(Double.MAX_VALUE, Double.MAX_VALUE));
        assertEquals(0D, MathUtil.getAngleBetweenRotations(Double.MAX_VALUE, 0D, canonical, 0D), 1E-6D);
        assertEquals(2D, MathUtil.signedYawDelta(-179D, 179D));
    }

    @Test
    void statisticsHandleEmptyConstantAndLaggedSamples()
    {
        final double[] values = {4D, 1D, 3D, 2D};
        assertEquals(2.5D, DataUtil.median(values));
        assertArrayEquals(new double[]{4D, 1D, 3D, 2D}, values);
        assertTrue(Double.isNaN(DataUtil.median(new double[0])));
        assertTrue(Double.isNaN(DataUtil.coefficientOfVariation(new double[]{1D})));
        assertEquals(0.5D, DataUtil.coefficientOfVariation(new double[]{1D, 2D, 3D}), 1E-12D);
        assertEquals(0D, DataUtil.correlation(new double[]{1D, 1D}, new double[]{1D, 2D}));
        assertEquals(-1D, DataUtil.correlation(new double[]{1D, 2D, 3D}, new double[]{3D, 2D, 1D}), 1E-12D);
        assertEquals(1D, DataUtil.correlation(new double[]{1D, 3D, 7D, 15D}, new double[]{1D, 3D, 7D, 15D}, 1), 1E-12D);
        assertThrows(IllegalArgumentException.class, () -> DataUtil.correlation(values, values, -1));
    }

    @Test
    void quadraticDetrendingRemovesTrendWithoutChangingInput()
    {
        final double[] values = new double[48];
        for (int i = 0; i < values.length; i++) values[i] = 8D + 0.2D * i + 0.01D * i * i;
        final double[] original = values.clone();
        assertArrayEquals(new double[48], TimeSeriesUtil.detrendQuadratic(values), 1E-12D);
        assertArrayEquals(original, values);
        assertThrows(IllegalArgumentException.class, () -> TimeSeriesUtil.detrendQuadratic(new double[2]));
        assertEquals(0D, TimeSeriesUtil.permutationEntropy(new double[]{1D, 2D, 3D, 4D}));
        assertEquals(1D, TimeSeriesUtil.signChangeRatio(new double[]{-1D, 1D, -1D, 1D}));
    }
}
