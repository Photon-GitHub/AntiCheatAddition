package de.photon.anticheataddition.user;

import de.photon.anticheataddition.modules.checks.targeting.TargetingAnalysis;
import de.photon.anticheataddition.modules.checks.targeting.TargetingContext;
import de.photon.anticheataddition.user.data.subdata.TargetingReplayData;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class TargetingReplayDataTest
{
    private static final int SAMPLE_COUNT = 48;

    @Test
    public void detectsExactNonOverlappingReplay()
    {
        final TargetingReplayData data = new TargetingReplayData();
        final TargetingAnalysis.Result result = randomResult(1L);

        assertEquals(0, data.compareAndRemember(TargetingContext.COMBAT, 1L, 48L, result).repeatedAxes());
        assertEquals(2, data.compareAndRemember(TargetingContext.COMBAT, 57L, 104L, result).repeatedAxes());
    }

    @Test
    public void detectsLowAmplitudeReplayWithoutAmplitudeExemption()
    {
        final TargetingReplayData data = new TargetingReplayData();
        final TargetingAnalysis.Result result = randomResult(3L, 0.003D, 0.002D);

        assertEquals(0, data.compareAndRemember(TargetingContext.COMBAT, 1L, 48L, result).repeatedAxes());
        assertEquals(2, data.compareAndRemember(TargetingContext.COMBAT, 57L, 104L, result).repeatedAxes());
    }

    @Test
    public void ignoresOverlappingSnapshots()
    {
        final TargetingReplayData data = new TargetingReplayData();
        final TargetingAnalysis.Result result = randomResult(1L);

        data.compareAndRemember(TargetingContext.COMBAT, 1L, 48L, result);
        assertEquals(0, data.compareAndRemember(TargetingContext.COMBAT, 9L, 56L, result).repeatedAxes());
    }

    @Test
    public void keepsCombatAndScaffoldHistoriesSeparate()
    {
        final TargetingReplayData data = new TargetingReplayData();
        final TargetingAnalysis.Result result = randomResult(1L);

        data.compareAndRemember(TargetingContext.COMBAT, 1L, 48L, result);
        assertEquals(0, data.compareAndRemember(TargetingContext.SCAFFOLD, 57L, 104L, result).repeatedAxes());
    }

    @Test
    public void doesNotMatchIndependentNaturalTraces()
    {
        final TargetingReplayData data = new TargetingReplayData();
        data.compareAndRemember(TargetingContext.COMBAT, 1L, 48L, randomResult(1L));
        assertEquals(0,
                     data.compareAndRemember(TargetingContext.COMBAT, 57L, 104L, randomResult(2L)).repeatedAxes());
    }

    private static TargetingAnalysis.Result randomResult(final long seed)
    {
        final Random random = new Random(seed);
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            yaw[i] = 15D + 0.6D * i + 0.008D * i * i + random.nextGaussian() * 0.2D;
            pitch[i] = 8D - 0.12D * i + 0.002D * i * i + random.nextGaussian() * 0.15D;
        }
        return TargetingAnalysis.analyze(yaw, pitch);
    }

    private static TargetingAnalysis.Result randomResult(final long seed,
                                                         final double yawAmplitude,
                                                         final double pitchAmplitude)
    {
        final Random random = new Random(seed);
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            yaw[i] = 15D + 0.6D * i + 0.008D * i * i + random.nextGaussian() * yawAmplitude;
            pitch[i] = 8D - 0.12D * i + 0.002D * i * i + random.nextGaussian() * pitchAmplitude;
        }
        return TargetingAnalysis.analyze(yaw, pitch);
    }
}
