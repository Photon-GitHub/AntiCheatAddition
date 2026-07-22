package de.photon.anticheataddition.modules;

import de.photon.anticheataddition.modules.checks.targeting.TargetingAnalysis;
import de.photon.anticheataddition.modules.checks.targeting.TargetingSwitchAnalysis;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class TargetingSwitchAnalysisTest
{
    private static final int SAMPLE_COUNT = 48;

    @Test
    public void detectsCleanSectionWindowPoisoning()
    {
        final Random random = new Random(0L);
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            yaw[i] = baseYaw(i) + (i < 16 ? 0D : (random.nextDouble() * 2D - 1D) * 0.28D);
            pitch[i] = basePitch(i) + Math.sin(i * 0.21D) * 0.02D;
        }

        final TargetingSwitchAnalysis.Result result = TargetingSwitchAnalysis.analyze(TargetingAnalysis.analyze(yaw, pitch));
        assertTrue(result.yaw().switching());
        assertEquals(1, result.switchingAxisCount());
    }

    @Test
    public void detectsLowAmplitudeWindowPoisoningWithoutAmplitudeExemption()
    {
        final Random random = new Random(14L);
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            yaw[i] = baseYaw(i) + (i < 16 ? 0D : (random.nextDouble() * 2D - 1D) * 0.009D);
            pitch[i] = basePitch(i);
        }

        final TargetingSwitchAnalysis.Result result = TargetingSwitchAnalysis.analyze(TargetingAnalysis.analyze(yaw, pitch));
        assertTrue(result.yaw().switching());
    }

    @Test
    public void detectsAlternatingRandomizedAxes()
    {
        final Random random = new Random(8L);
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            final int phase = i / 16;
            yaw[i] = baseYaw(i) + (phase == 1 ? 0D : (random.nextDouble() * 2D - 1D) * 0.28D);
            pitch[i] = basePitch(i) + (phase == 1 ? (random.nextDouble() * 2D - 1D) * 0.22D : 0D);
        }

        final TargetingSwitchAnalysis.Result result = TargetingSwitchAnalysis.analyze(TargetingAnalysis.analyze(yaw, pitch));
        assertTrue(result.switchingAxisCount() >= 1);
    }

    @Test
    public void ignoresCorrelatedHumanMovement()
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

        final TargetingSwitchAnalysis.Result result = TargetingSwitchAnalysis.analyze(TargetingAnalysis.analyze(yaw, pitch));
        assertEquals(0, result.switchingAxisCount());
    }

    @Test
    public void ignoresQuantizedMouseMovement()
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

        final TargetingSwitchAnalysis.Result result = TargetingSwitchAnalysis.analyze(TargetingAnalysis.analyze(yaw, pitch));
        assertEquals(0, result.switchingAxisCount());
    }

    private static double baseYaw(final int index)
    {
        return 15D + 0.6D * index + 0.008D * index * index;
    }

    private static double basePitch(final int index)
    {
        return 8D - 0.12D * index + 0.002D * index * index;
    }
}
