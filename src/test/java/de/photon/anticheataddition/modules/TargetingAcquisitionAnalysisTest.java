package de.photon.anticheataddition.modules;

import de.photon.anticheataddition.modules.checks.targeting.TargetingAcquisitionAnalysis;
import de.photon.anticheataddition.user.data.subdata.TargetingData;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class TargetingAcquisitionAnalysisTest
{
    private static final TargetingAcquisitionAnalysis.TargetBox TARGET_BOX =
            new TargetingAcquisitionAnalysis.TargetBox(-0.45D, -0.15D, 5.55D, 0.45D, 2D, 6.45D);

    @Test
    public void extractsFixedBoundarySlowdown()
    {
        final TargetingAcquisitionAnalysis.Result result = TargetingAcquisitionAnalysis.analyze(
                assistSnapshot(1L, 18, 20D, 6D),
                TARGET_BOX,
                1.62D);

        assertTrue(result.valid());
        assertTrue(result.profile().controlledSlowdownCandidate());
        assertTrue(result.profile().activationError() > 3D);
        assertTrue(result.profile().activationError() < 7D);
        assertTrue(result.profile().slowdownRatio() < 0.5D);
    }

    @Test
    public void followsThePlayersFinalHitPointInsteadOfEntityCenter()
    {
        final int sampleCount = 18;
        final double[] yaw = new double[sampleCount];
        final double[] pitch = new double[sampleCount];
        double currentYaw = 18D;
        for (int i = 0; i < sampleCount; i++) {
            yaw[i] = currentYaw;
            pitch[i] = 5.5D;
            final double targetYaw = -3.3D;
            final double multiplier = Math.abs(currentYaw - targetYaw) <= 6D ? 0.35D : 1D;
            currentYaw += (targetYaw - currentYaw) * 0.23D * multiplier;
        }
        yaw[sampleCount - 1] = -3.3D;

        final TargetingAcquisitionAnalysis.Result result = TargetingAcquisitionAnalysis.analyze(snapshot(yaw, pitch),
                                                                                                TARGET_BOX,
                                                                                                1.62D);
        assertTrue(result.valid());
        assertTrue(result.profile().controlledSlowdownCandidate());
    }

    @Test
    public void rejectsAPlayerWhoWasAlreadyOnTarget()
    {
        final double[] yaw = new double[16];
        final double[] pitch = new double[16];
        for (int i = 0; i < yaw.length; i++) pitch[i] = 5.9D;

        final TargetingAcquisitionAnalysis.Result result = TargetingAcquisitionAnalysis.analyze(snapshot(yaw, pitch),
                                                                                                TARGET_BOX,
                                                                                                1.62D);
        assertFalse(result.valid());
    }

    @Test
    public void rejectsMovementWhichDoesNotSustainProgressTowardTheTarget()
    {
        final Random random = new Random(3L);
        final double[] yaw = new double[18];
        final double[] pitch = new double[18];
        for (int i = 0; i < yaw.length; i++) {
            yaw[i] = 12D + random.nextGaussian() * 5D;
            pitch[i] = 5.9D + random.nextGaussian();
        }
        yaw[yaw.length - 1] = 0D;
        pitch[pitch.length - 1] = 5.9D;

        final TargetingAcquisitionAnalysis.Result result = TargetingAcquisitionAnalysis.analyze(snapshot(yaw, pitch),
                                                                                                TARGET_BOX,
                                                                                                1.62D);
        assertFalse(result.valid());
    }

    @Test
    public void ordinaryMinimumJerkAcquisitionIsNotEnoughByItself()
    {
        final TargetingAcquisitionAnalysis.Result result = TargetingAcquisitionAnalysis.analyze(
                humanSnapshot(9L, 18, 20D),
                TARGET_BOX,
                1.62D);

        assertTrue(result.valid());
        // A single profile may look smooth, but it is never a violation. The cross-acquisition history decides that.
        assertTrue(result.profile().approachEfficiency() > 0.5D);
    }

    private static TargetingData.AcquisitionSnapshot assistSnapshot(final long seed,
                                                                    final int sampleCount,
                                                                    final double initialYaw,
                                                                    final double activationError)
    {
        final Random random = new Random(seed);
        final double[] yaw = new double[sampleCount];
        final double[] pitch = new double[sampleCount];
        double currentYaw = initialYaw;
        double velocity = Math.max(0.8D, initialYaw / (sampleCount * 0.45D));
        for (int i = 0; i < sampleCount; i++) {
            yaw[i] = currentYaw + random.nextGaussian() * 0.004D;
            pitch[i] = 5.9D + random.nextGaussian() * 0.003D;
            final double multiplier = Math.abs(currentYaw) <= activationError ? 0.38D : 1D;
            currentYaw = Math.max(0D, currentYaw - velocity * multiplier);
            velocity = 0.90D * velocity + 0.10D * Math.max(0.35D, currentYaw * 0.22D);
        }
        yaw[sampleCount - 1] = 0D;
        pitch[sampleCount - 1] = 5.9D;
        return snapshot(yaw, pitch);
    }

    private static TargetingData.AcquisitionSnapshot humanSnapshot(final long seed,
                                                                   final int sampleCount,
                                                                   final double initialYaw)
    {
        final Random random = new Random(seed);
        final double[] yaw = new double[sampleCount];
        final double[] pitch = new double[sampleCount];
        final double overshoot = random.nextGaussian() * 1.2D;
        double correlated = 0D;
        for (int i = 0; i < sampleCount; i++) {
            final double progress = i / (double) (sampleCount - 1);
            final double minimumJerk = progress * progress * progress *
                                       (10D - 15D * progress + 6D * progress * progress);
            correlated = 0.68D * correlated + random.nextGaussian() * 0.16D;
            yaw[i] = initialYaw * (1D - minimumJerk) +
                     overshoot * Math.sin(Math.PI * progress) * progress +
                     correlated;
            pitch[i] = 5.9D + random.nextGaussian() * 0.08D;
        }
        yaw[sampleCount - 1] = random.nextGaussian() * 0.12D;
        pitch[sampleCount - 1] = 5.9D + random.nextGaussian() * 0.06D;
        return snapshot(yaw, pitch);
    }

    private static TargetingData.AcquisitionSnapshot snapshot(final double[] yaw, final double[] pitch)
    {
        final int sampleCount = yaw.length;
        final double[] x = new double[sampleCount];
        final double[] y = new double[sampleCount];
        final double[] z = new double[sampleCount];
        final long[] sequence = new long[sampleCount];
        for (int i = 0; i < sampleCount; i++) sequence[i] = i + 1L;
        return new TargetingData.AcquisitionSnapshot(x, y, z, yaw, pitch, sequence);
    }
}
