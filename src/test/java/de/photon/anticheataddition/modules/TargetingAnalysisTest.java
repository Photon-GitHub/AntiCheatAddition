package de.photon.anticheataddition.modules;

import de.photon.anticheataddition.modules.checks.targeting.TargetingAnalysis;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public final class TargetingAnalysisTest
{
    private static final int SAMPLE_COUNT = 48;

    @Test
    public void detectsUniformNoiseOnOnlyYaw()
    {
        final RotationSample sample = randomizedSample(1L, NoiseType.UNIFORM, true, false);
        final TargetingAnalysis.Result result = TargetingAnalysis.analyze(sample.yaw(), sample.pitch());

        assertTrue(result.yaw().pattern().isRandomized());
        assertEquals(TargetingAnalysis.Pattern.PRECISE, result.pitch().pattern());
        assertEquals(1, result.randomizedAxisCount());
        assertEquals(1, result.preciseAxisCount());
    }

    @Test
    public void detectsGaussianNoiseOnOnlyPitch()
    {
        final RotationSample sample = randomizedSample(1L, NoiseType.GAUSSIAN, false, true);
        final TargetingAnalysis.Result result = TargetingAnalysis.analyze(sample.yaw(), sample.pitch());

        assertEquals(TargetingAnalysis.Pattern.PRECISE, result.yaw().pattern());
        assertTrue(result.pitch().pattern().isRandomized());
        assertEquals(1, result.randomizedAxisCount());
    }

    @Test
    public void detectsDistributionFreeLaplaceNoise()
    {
        final RotationSample sample = randomizedSample(1L, NoiseType.LAPLACE, true, false);
        final TargetingAnalysis.Result result = TargetingAnalysis.analyze(sample.yaw(), sample.pitch());

        assertEquals(TargetingAnalysis.Pattern.DISTRIBUTION_FREE, result.yaw().pattern());
        assertEquals(1, result.randomizedAxisCount());
    }

    @Test
    public void detectsDistributionFreeBimodalNoise()
    {
        final RotationSample sample = randomizedSample(1L, NoiseType.BIMODAL, true, false);
        final TargetingAnalysis.Result result = TargetingAnalysis.analyze(sample.yaw(), sample.pitch());

        assertEquals(TargetingAnalysis.Pattern.DISTRIBUTION_FREE, result.yaw().pattern());
    }

    @Test
    public void detectsPeriodicOffsetTable()
    {
        final double[] motif = {-0.3D, 0.12D, 0.28D, -0.08D, 0.21D, -0.25D, 0.02D};
        final RotationSample sample = patternedSample(i -> motif[i % motif.length]);
        final TargetingAnalysis.Result result = TargetingAnalysis.analyze(sample.yaw(), sample.pitch());

        assertEquals(TargetingAnalysis.SyntheticPattern.PERIODIC, result.yaw().syntheticPattern());
        assertEquals(1, result.syntheticAxisCount());
    }

    @Test
    public void detectsSineWaveOffsets()
    {
        final RotationSample sample = patternedSample(i -> Math.sin(i * Math.PI / 4D) * 0.28D);
        final TargetingAnalysis.Result result = TargetingAnalysis.analyze(sample.yaw(), sample.pitch());

        assertEquals(TargetingAnalysis.SyntheticPattern.PERIODIC, result.yaw().syntheticPattern());
    }

    @Test
    public void detectsAlternatingOffsets()
    {
        final RotationSample sample = patternedSample(i -> (i & 1) == 0 ? 0.24D : -0.24D);
        final TargetingAnalysis.Result result = TargetingAnalysis.analyze(sample.yaw(), sample.pitch());

        assertEquals(TargetingAnalysis.SyntheticPattern.ALTERNATING, result.yaw().syntheticPattern());
    }

    @Test
    public void detectsNoiseDespiteAShortCleanSection()
    {
        final Random random = new Random(1L);
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            yaw[i] = baseYaw(i) + (i < 16 ? 0D : (random.nextDouble() * 2D - 1D) * 0.28D);
            pitch[i] = basePitch(i);
        }

        final TargetingAnalysis.Result result = TargetingAnalysis.analyze(yaw, pitch);
        assertTrue(result.yaw().pattern().isRandomized());
        assertTrue(result.yaw().randomWindowCount() >= 2);
    }

    @Test
    public void detectsOnePerfectAxis()
    {
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        final Random random = new Random(10L);

        for (int i = 0; i < SAMPLE_COUNT; i++) {
            yaw[i] = baseYaw(i);
            pitch[i] = basePitch(i) + correlatedNoise(random, i);
        }

        final TargetingAnalysis.Result result = TargetingAnalysis.analyze(yaw, pitch);
        assertEquals(TargetingAnalysis.Pattern.PRECISE, result.yaw().pattern());
        assertEquals(1, result.preciseAxisCount());
    }

    @Test
    public void detectsBothPerfectAxes()
    {
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            yaw[i] = baseYaw(i);
            pitch[i] = basePitch(i);
        }

        final TargetingAnalysis.Result result = TargetingAnalysis.analyze(yaw, pitch);
        assertEquals(2, result.preciseAxisCount());
    }

    @Test
    public void detectsLowAmplitudeGaussianNoiseWithoutAmplitudeExemption()
    {
        final Random random = new Random(23L);
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];

        for (int i = 0; i < SAMPLE_COUNT; i++) {
            yaw[i] = baseYaw(i) + random.nextGaussian() * 0.035D;
            pitch[i] = basePitch(i);
        }

        final TargetingAnalysis.Result result = TargetingAnalysis.analyze(yaw, pitch);
        assertTrue(result.yaw().pattern().isRandomized());
    }

    @Test
    public void detectsLongPeriodicTableBeyondTheFormerTwelveSampleSearchLimit()
    {
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        final int period = 13;
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            final int phase = i % period;
            final double generatedOffset = Math.sin(2D * Math.PI * phase / period) * 0.08D + phase * 0.003D;
            yaw[i] = baseYaw(i) + generatedOffset;
            pitch[i] = basePitch(i);
        }

        final TargetingAnalysis.Result result = TargetingAnalysis.analyze(yaw, pitch);
        assertEquals(TargetingAnalysis.SyntheticPattern.PERIODIC, result.yaw().syntheticPattern());
        assertEquals(13, result.yaw().periodicLag());
    }

    @Test
    public void detectsLowAmplitudePeriodicNoiseWithoutAmplitudeExemption()
    {
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        final double[] pattern = {0.0018D, -0.0011D, 0.0024D, -0.002D};
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            yaw[i] = baseYaw(i) + pattern[i % pattern.length];
            pitch[i] = basePitch(i);
        }

        final TargetingAnalysis.Result result = TargetingAnalysis.analyze(yaw, pitch);
        assertNotSame(result.yaw().syntheticPattern(), TargetingAnalysis.SyntheticPattern.NONE);
    }

    @Test
    public void combinesRandomnessMetricsInsteadOfUsingOneHardAutocorrelationCutoff()
    {
        final Random random = new Random(136L);
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        double yawError = 0D;

        for (int i = 0; i < SAMPLE_COUNT; i++) {
            // This residual intentionally sits just above the former lag-one cut-off. The other independent
            // randomness metrics remain strong, so moving one published metric across one boundary is insufficient.
            yawError = 0.28D * yawError + random.nextGaussian() * 0.08D;
            yaw[i] = baseYaw(i) + yawError;
            pitch[i] = basePitch(i);
        }

        final TargetingAnalysis.Result result = TargetingAnalysis.analyze(yaw, pitch);
        assertTrue(Math.abs(result.yaw().lagOneAutocorrelation()) > 0.25D);
        assertTrue(result.yaw().pattern().isRandomized());
    }

    @Test
    public void doesNotFlagTemporallyCorrelatedHumanMovement()
    {
        final Random random = new Random(0L);
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        double yawError = 0D;
        double pitchError = 0D;

        for (int i = 0; i < SAMPLE_COUNT; i++) {
            yawError = 0.82D * yawError + random.nextGaussian() * 0.08D;
            pitchError = 0.82D * pitchError + random.nextGaussian() * 0.05D;
            yaw[i] = baseYaw(i) + yawError;
            pitch[i] = basePitch(i) + pitchError;
        }

        final TargetingAnalysis.Result result = TargetingAnalysis.analyze(yaw, pitch);
        assertEquals(0, result.randomizedAxisCount());
        assertEquals(0, result.syntheticAxisCount());
        assertEquals(0, result.preciseAxisCount());
    }

    @Test
    public void doesNotFlagQuantizedMouseMovement()
    {
        final Random random = new Random(0L);
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        double currentYaw = 10D;
        double currentPitch = 5D;
        double yawVelocity = 0D;
        double pitchVelocity = 0D;

        for (int i = 0; i < SAMPLE_COUNT; i++) {
            yawVelocity = 0.72D * yawVelocity + 0.25D + random.nextGaussian() * 0.12D;
            pitchVelocity = 0.72D * pitchVelocity - 0.04D + random.nextGaussian() * 0.07D;
            currentYaw += Math.rint(yawVelocity / 0.12D) * 0.12D;
            currentPitch += Math.rint(pitchVelocity / 0.12D) * 0.12D;
            yaw[i] = currentYaw;
            pitch[i] = currentPitch;
        }

        final TargetingAnalysis.Result result = TargetingAnalysis.analyze(yaw, pitch);
        assertEquals(0, result.randomizedAxisCount());
        assertEquals(0, result.syntheticAxisCount());
        assertEquals(0, result.preciseAxisCount());
    }

    @Test
    public void unwrapsYawAcrossTheBoundary()
    {
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            double value = 174D + i * 0.7D;
            while (value > 180D) value -= 360D;
            yaw[i] = value;
            pitch[i] = basePitch(i);
        }

        final TargetingAnalysis.Result result = TargetingAnalysis.analyze(yaw, pitch);
        assertEquals(TargetingAnalysis.Pattern.PRECISE, result.yaw().pattern());
        assertFalse(result.yaw().pattern().isRandomized());
    }

    @Test
    public void detectsNoiseDespiteInsertedLargeLevelShift()
    {
        final Random random = new Random(4L);
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            final double discontinuity = i >= 20 ? 120D : 0D;
            yaw[i] = baseYaw(i) + discontinuity + (random.nextDouble() * 2D - 1D) * 0.28D;
            pitch[i] = basePitch(i);
        }

        final TargetingAnalysis.Result result = TargetingAnalysis.analyze(yaw, pitch);
        assertTrue(result.yaw().pattern().isRandomized());
    }


    @Test
    public void doesNotTreatVanillaPitchClampAsPrecision()
    {
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            yaw[i] = baseYaw(i);
            pitch[i] = 90D;
        }

        final TargetingAnalysis.Result result = TargetingAnalysis.analyze(yaw, pitch);
        assertEquals(TargetingAnalysis.Pattern.NATURAL, result.pitch().pattern());
    }

    @Test
    public void highAmplitudeUniformNoiseHasNoUpperExemption()
    {
        final Random random = new Random(17L);
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            yaw[i] = baseYaw(i) + (random.nextDouble() * 2D - 1D) * 4D;
            pitch[i] = basePitch(i);
        }

        final TargetingAnalysis.Result result = TargetingAnalysis.analyze(yaw, pitch);
        assertTrue(result.yaw().pattern().isRandomized());
    }

    @Test
    public void justBelowDiscontinuityFloorStillReachesPatternAnalysis()
    {
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            yaw[i] = baseYaw(i) + ((i & 1) == 0 ? 12D : -12D);
            pitch[i] = basePitch(i);
        }

        final TargetingAnalysis.Result result = TargetingAnalysis.analyze(yaw, pitch);
        assertEquals(TargetingAnalysis.SyntheticPattern.ALTERNATING, result.yaw().syntheticPattern());
    }

    @Test
    public void trustedBoundaryPreservesRandomizedEvidence()
    {
        final Random random = new Random(31L);
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        final boolean[] trustedBreaks = new boolean[SAMPLE_COUNT];
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            final double serverShift = i >= 24 ? 150D : 0D;
            yaw[i] = baseYaw(i) + serverShift + (random.nextDouble() * 2D - 1D) * 0.28D;
            pitch[i] = basePitch(i);
        }
        trustedBreaks[24] = true;

        final TargetingAnalysis.Result result = TargetingAnalysis.analyze(yaw, pitch, trustedBreaks);
        assertTrue(result.yaw().pattern().isRandomized());
    }

    @Test
    public void trustedBoundaryDoesNotCreateRandomizedEvidence()
    {
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        final boolean[] trustedBreaks = new boolean[SAMPLE_COUNT];
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            final double serverShift = i >= 24 ? 140D : 0D;
            yaw[i] = baseYaw(i) + serverShift;
            pitch[i] = basePitch(i) + (i >= 24 ? 20D : 0D);
        }
        trustedBreaks[24] = true;

        final TargetingAnalysis.Result result = TargetingAnalysis.analyze(yaw, pitch, trustedBreaks);
        assertEquals(0, result.randomizedAxisCount());
        assertEquals(0, result.syntheticAxisCount());
    }

    @Test
    public void handlesExtremeFiniteYawWithoutOverflow()
    {
        final double delta = TargetingAnalysis.signedYawDelta(Double.MAX_VALUE, -Double.MAX_VALUE);
        assertTrue(Double.isFinite(delta));
        assertTrue(delta >= -180D && delta <= 180D);
        assertTrue(Double.isFinite(TargetingAnalysis.normalizeYaw(Double.MAX_VALUE)));
    }

    private static RotationSample randomizedSample(final long seed,
                                                   final NoiseType type,
                                                   final boolean randomizeYaw,
                                                   final boolean randomizePitch)
    {
        final Random random = new Random(seed);
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];

        for (int i = 0; i < SAMPLE_COUNT; i++) {
            yaw[i] = baseYaw(i) + (randomizeYaw ? noise(random, type, 0.28D) : 0D);
            pitch[i] = basePitch(i) + (randomizePitch ? noise(random, type, 0.18D) : 0D);
        }
        return new RotationSample(yaw, pitch);
    }

    private static RotationSample patternedSample(final OffsetFunction function)
    {
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            yaw[i] = baseYaw(i) + function.offset(i);
            pitch[i] = basePitch(i) + Math.sin(i * 0.19D) * 0.02D;
        }
        return new RotationSample(yaw, pitch);
    }

    private static double noise(final Random random, final NoiseType type, final double scale)
    {
        return switch (type) {
            case UNIFORM -> (random.nextDouble() * 2D - 1D) * scale;
            case GAUSSIAN -> random.nextGaussian() * scale;
            case LAPLACE -> {
                final double uniform = random.nextDouble() - 0.5D;
                yield -scale * 0.65D * Math.signum(uniform) * Math.log(1D - 2D * Math.abs(uniform));
            }
            case BIMODAL -> (random.nextBoolean() ? scale : -scale) + random.nextGaussian() * scale * 0.04D;
        };
    }

    private static double correlatedNoise(final Random random, final int index)
    {
        return Math.sin(index * 0.35D) * 0.08D + random.nextGaussian() * 0.005D;
    }

    private static double baseYaw(final int index)
    {
        return 15D + 0.6D * index + 0.008D * index * index;
    }

    private static double basePitch(final int index)
    {
        return 8D - 0.12D * index + 0.002D * index * index;
    }

    private enum NoiseType
    {
        UNIFORM,
        GAUSSIAN,
        LAPLACE,
        BIMODAL
    }

    private interface OffsetFunction
    {
        double offset(int index);
    }

    private record RotationSample(double[] yaw, double[] pitch)
    {
    }
}
