package de.photon.anticheataddition.modules;

import de.photon.anticheataddition.modules.checks.targeting.TargetingAnalysis;
import de.photon.anticheataddition.modules.checks.targeting.TargetingDiscontinuityAnalysis;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class TargetingDiscontinuityAnalysisTest
{
    private static final int SAMPLE_COUNT = 48;

    @Test
    public void detectsRepeatedOppositeDirectionYawJumps()
    {
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        double currentYaw = 0D;
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            if (i > 0 && i % 4 == 0) currentYaw += (i / 4 & 1) == 0 ? 100D : -100D;
            else currentYaw += 0.4D;
            yaw[i] = TargetingAnalysis.normalizeYaw(currentYaw);
            pitch[i] = i * 0.02D;
        }

        final TargetingDiscontinuityAnalysis.Result result = TargetingDiscontinuityAnalysis.analyze(yaw, pitch);
        assertTrue(result.yaw().suspicious());
        assertEquals(1, result.suspiciousAxisCount());
    }

    @Test
    public void detectsRepeatedPitchJumps()
    {
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        double currentPitch = 0D;
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            yaw[i] = i * 0.3D;
            if (i > 0 && i % 4 == 0) currentPitch += (i / 4 & 1) == 0 ? 35D : -35D;
            else currentPitch += 0.05D;
            pitch[i] = currentPitch;
        }

        final TargetingDiscontinuityAnalysis.Result result = TargetingDiscontinuityAnalysis.analyze(yaw, pitch);
        assertTrue(result.pitch().suspicious());
    }

    @Test
    public void detectsContinuouslyVariedLargeJumpsWhichInflateTheRobustBaseline()
    {
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        final double[] jumps = {27D, -55D, 90D, -38D, 74D, -110D, 31D, -82D, 65D, -45D, 120D, -29D};
        double currentYaw = 0D;

        for (int i = 0; i < SAMPLE_COUNT; i++) {
            if (i > 0) currentYaw += jumps[(i - 1) % jumps.length];
            yaw[i] = TargetingAnalysis.normalizeYaw(currentYaw);
            pitch[i] = 0D;
        }

        final TargetingDiscontinuityAnalysis.Result result = TargetingDiscontinuityAnalysis.analyze(yaw, pitch);
        assertTrue(result.yaw().suspicious());
        assertTrue(result.yaw().severe());
    }

    @Test
    public void ignoresServerAuthoritativeJumpTransitions()
    {
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        final boolean[] trustedBreaks = new boolean[SAMPLE_COUNT];
        double currentYaw = 0D;
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            if (i > 0 && i % 4 == 0) {
                currentYaw += (i / 4 & 1) == 0 ? 100D : -100D;
                trustedBreaks[i] = true;
            } else {
                currentYaw += 0.4D;
            }
            yaw[i] = TargetingAnalysis.normalizeYaw(currentYaw);
            pitch[i] = i * 0.02D;
        }

        final TargetingDiscontinuityAnalysis.Result result =
                TargetingDiscontinuityAnalysis.analyze(yaw, pitch, trustedBreaks);
        assertEquals(0, result.suspiciousAxisCount());
    }

    @Test
    public void ignoresSingleFastTurn()
    {
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            yaw[i] = i * 0.5D + (i >= 24 ? 120D : 0D);
            pitch[i] = i * 0.03D;
        }

        final TargetingDiscontinuityAnalysis.Result result = TargetingDiscontinuityAnalysis.analyze(yaw, pitch);
        assertFalse(result.yaw().suspicious());
        assertEquals(0, result.suspiciousAxisCount());
    }

    @Test
    public void ignoresFastMovementInOneDirection()
    {
        final double[] yaw = new double[SAMPLE_COUNT];
        final double[] pitch = new double[SAMPLE_COUNT];
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            yaw[i] = TargetingAnalysis.normalizeYaw(i * 30D);
            pitch[i] = 0D;
        }

        final TargetingDiscontinuityAnalysis.Result result = TargetingDiscontinuityAnalysis.analyze(yaw, pitch);
        assertEquals(0, result.suspiciousAxisCount());
    }
}
