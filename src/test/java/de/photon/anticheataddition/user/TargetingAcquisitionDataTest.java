package de.photon.anticheataddition.user;

import de.photon.anticheataddition.modules.checks.targeting.TargetingAcquisitionAnalysis;
import de.photon.anticheataddition.user.data.subdata.TargetingAcquisitionData;
import de.photon.anticheataddition.user.data.subdata.TargetingData;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public final class TargetingAcquisitionDataTest
{
    @Test
    public void neverTreatsOneSmoothAcquisitionAsSufficientEvidence()
    {
        final TargetingAcquisitionData data = new TargetingAcquisitionData();
        final TargetingAcquisitionData.Assessment assessment = data.add(assistProfile(1L, 6D));

        assertFalse(assessment.enoughData());
        assertFalse(assessment.suspicious());
    }

    @Test
    public void detectsRepeatedFixedTargetBoundarySuppression()
    {
        final TargetingAcquisitionData data = new TargetingAcquisitionData();
        final Random random = new Random(2L);
        TargetingAcquisitionData.Assessment assessment = null;
        for (int i = 0; i < 32; i++) {
            assessment = data.add(assistProfile(random.nextLong(), 5.8D + random.nextGaussian() * 0.12D));
        }

        assertNotNull(assessment);
        assertTrue(assessment.enoughData());
        assertTrue(assessment.suspicious());
    }

    @Test
    public void doesNotFlagVariableHumanAcquisitionProfiles()
    {
        final TargetingAcquisitionData data = new TargetingAcquisitionData();
        final Random random = new Random(3L);
        TargetingAcquisitionData.Assessment assessment = null;
        for (int i = 0; i < 64; i++) assessment = data.add(humanProfile(random));

        assertNotNull(assessment);
        assertTrue(assessment.enoughData());
        assertFalse(assessment.suspicious());
    }


    @Test
    public void repeatedMinimumJerkAcquisitionsDoNotBecomeSuspicious()
    {
        assertFalse(runLegitimateSession(11L, LegitimateModel.MINIMUM_JERK));
    }

    @Test
    public void repeatedSkilledSmoothAcquisitionsDoNotBecomeSuspicious()
    {
        assertFalse(runLegitimateSession(12L, LegitimateModel.SKILLED));
    }

    @Test
    public void repeatedQuantizedAcquisitionsDoNotBecomeSuspicious()
    {
        assertFalse(runLegitimateSession(13L, LegitimateModel.QUANTIZED));
    }

    @Test
    public void doesNotCombineIncomparableTargetDistances()
    {
        final TargetingAcquisitionData data = new TargetingAcquisitionData();
        TargetingAcquisitionData.Assessment assessment = null;
        for (int i = 0; i < 20; i++) {
            final TargetingAcquisitionAnalysis.Profile profile = assistProfile(i, 6D);
            final double distance = (i & 1) == 0 ? 2D : 7D;
            assessment = data.add(copyWithDistance(profile, distance));
        }

        assertNotNull(assessment);
        assertFalse(assessment.enoughData());
    }


    private static boolean runLegitimateSession(final long seed, final LegitimateModel model)
    {
        final TargetingAcquisitionAnalysis.TargetBox targetBox =
                new TargetingAcquisitionAnalysis.TargetBox(-0.45D, -0.15D, 5.55D, 0.45D, 2D, 6.45D);
        final TargetingAcquisitionData data = new TargetingAcquisitionData();
        final Random random = new Random(seed);
        for (int acquisition = 0; acquisition < 300; acquisition++) {
            final int sampleCount = 14 + random.nextInt(8);
            final double initialYaw = 10D + random.nextDouble() * 16D;
            final TargetingData.AcquisitionSnapshot snapshot = switch (model) {
                case MINIMUM_JERK -> minimumJerkSnapshot(random, sampleCount, initialYaw);
                case SKILLED -> skilledSnapshot(random, sampleCount, initialYaw);
                case QUANTIZED -> quantizedSnapshot(random, sampleCount, initialYaw);
            };
            final TargetingAcquisitionAnalysis.Result result =
                    TargetingAcquisitionAnalysis.analyze(snapshot, targetBox, 1.62D);
            if (!result.valid()) continue;
            final TargetingAcquisitionData.Assessment assessment = data.add(result.profile());
            if (assessment.suspicious()) return true;
        }
        return false;
    }

    private static TargetingData.AcquisitionSnapshot minimumJerkSnapshot(final Random random,
                                                                         final int sampleCount,
                                                                         final double initialYaw)
    {
        final double[] yaw = new double[sampleCount];
        final double[] pitch = new double[sampleCount];
        final double overshoot = random.nextGaussian() * 1.6D;
        final double phase = random.nextDouble() * Math.PI * 2D;
        final double temporalWarp = 0.75D + random.nextDouble() * 0.5D;
        double correlated = 0D;
        for (int i = 0; i < sampleCount; i++) {
            final double rawProgress = i / (double) (sampleCount - 1);
            final double progress = Math.clamp(Math.pow(rawProgress, temporalWarp), 0D, 1D);
            final double minimumJerk = progress * progress * progress *
                                       (10D - 15D * progress + 6D * progress * progress);
            correlated = 0.55D * correlated + random.nextGaussian() * (0.12D + random.nextDouble() * 0.16D);
            yaw[i] = initialYaw * (1D - minimumJerk) +
                     overshoot * Math.sin(Math.PI * progress) * (0.3D + 0.7D * progress) +
                     correlated;
            pitch[i] = 5.9D + 0.25D * Math.sin(progress * 5D + phase) + random.nextGaussian() * 0.11D;
        }
        yaw[sampleCount - 1] = random.nextGaussian() * 0.18D;
        pitch[sampleCount - 1] = 5.9D + random.nextGaussian() * 0.09D;
        return snapshot(yaw, pitch);
    }

    private static TargetingData.AcquisitionSnapshot skilledSnapshot(final Random random,
                                                                     final int sampleCount,
                                                                     final double initialYaw)
    {
        final double[] yaw = new double[sampleCount];
        final double[] pitch = new double[sampleCount];
        final double shape = 1.1D + random.nextDouble() * 3.8D;
        final double phase = random.nextDouble() * Math.PI * 2D;
        double correlated = 0D;
        for (int i = 0; i < sampleCount; i++) {
            final double progress = i / (double) (sampleCount - 1);
            correlated = 0.45D * correlated + random.nextGaussian() * 0.045D;
            yaw[i] = initialYaw * Math.pow(1D - progress, shape) +
                     Math.sin(progress * (5D + random.nextDouble() * 4D) + phase) *
                     (0.025D + random.nextDouble() * 0.12D) +
                     correlated;
            pitch[i] = 5.9D + Math.sin(progress * 4D + phase) *
                              (0.02D + random.nextDouble() * 0.09D) + random.nextGaussian() * 0.025D;
        }
        yaw[sampleCount - 1] = random.nextGaussian() * 0.08D;
        pitch[sampleCount - 1] = 5.9D + random.nextGaussian() * 0.04D;
        return snapshot(yaw, pitch);
    }

    private static TargetingData.AcquisitionSnapshot quantizedSnapshot(final Random random,
                                                                       final int sampleCount,
                                                                       final double initialYaw)
    {
        final double[] yaw = new double[sampleCount];
        final double[] pitch = new double[sampleCount];
        final double quantum = 0.08D + random.nextDouble() * 0.18D;
        double currentYaw = initialYaw;
        double velocity = 0D;
        for (int i = 0; i < sampleCount; i++) {
            velocity = 0.50D * velocity +
                       (0.10D + random.nextDouble() * 0.08D) * currentYaw +
                       random.nextGaussian() * 0.42D;
            final double step = Math.rint(Math.min(Math.abs(currentYaw), Math.abs(velocity)) / quantum) * quantum;
            currentYaw -= Math.copySign(step, currentYaw);
            yaw[i] = currentYaw;
            pitch[i] = Math.rint((5.9D + random.nextGaussian() * 0.1D) / quantum) * quantum;
        }
        yaw[sampleCount - 1] = 0D;
        pitch[sampleCount - 1] = Math.rint(5.9D / quantum) * quantum;
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

    private enum LegitimateModel
    {
        MINIMUM_JERK,
        SKILLED,
        QUANTIZED
    }

    private static TargetingAcquisitionAnalysis.Profile assistProfile(final long seed,
                                                                      final double activationError)
    {
        final Random random = new Random(seed);
        final double[] profile = {
                1D + random.nextGaussian() * 0.015D,
                0.96D + random.nextGaussian() * 0.015D,
                0.90D + random.nextGaussian() * 0.015D,
                0.36D + random.nextGaussian() * 0.012D,
                0.24D + random.nextGaussian() * 0.012D
        };
        return new TargetingAcquisitionAnalysis.Profile(true,
                                                        15D + random.nextGaussian(),
                                                        0.08D,
                                                        0.31D + random.nextGaussian() * 0.018D,
                                                        activationError,
                                                        0.55D + random.nextGaussian() * 0.02D,
                                                        0.25D,
                                                        0.35D,
                                                        0.91D,
                                                        0.94D,
                                                        0.94D,
                                                        3.2D + random.nextGaussian() * 0.08D,
                                                        15,
                                                        profile);
    }

    private static TargetingAcquisitionAnalysis.Profile humanProfile(final Random random)
    {
        final double[] profile = new double[TargetingAcquisitionAnalysis.PROFILE_BIN_COUNT];
        double current = 0.5D + random.nextDouble();
        for (int i = 0; i < profile.length; i++) {
            current += random.nextGaussian() * 0.35D;
            profile[i] = Math.max(0.05D, current);
        }
        return new TargetingAcquisitionAnalysis.Profile(random.nextDouble() < 0.45D,
                                                        5D + random.nextDouble() * 20D,
                                                        random.nextDouble() * 0.5D,
                                                        0.25D + random.nextDouble() * 0.7D,
                                                        0.75D + random.nextDouble() * 7.25D,
                                                        0.18D + random.nextDouble() * 0.65D,
                                                        0.1D + random.nextDouble() * 0.4D,
                                                        0.2D + random.nextDouble() * 1.4D,
                                                        -0.4D + random.nextDouble() * 1.35D,
                                                        0.6D + random.nextDouble() * 0.4D,
                                                        0.62D + random.nextDouble() * 0.38D,
                                                        2.5D + random.nextDouble() * 1.5D,
                                                        8 + random.nextInt(14),
                                                        profile);
    }

    private static TargetingAcquisitionAnalysis.Profile copyWithDistance(
            final TargetingAcquisitionAnalysis.Profile profile,
            final double targetDistance)
    {
        return new TargetingAcquisitionAnalysis.Profile(profile.controlledSlowdownCandidate(),
                                                        profile.initialError(),
                                                        profile.finalError(),
                                                        profile.slowdownRatio(),
                                                        profile.activationError(),
                                                        profile.activationDrop(),
                                                        profile.meanGain(),
                                                        profile.gainVariation(),
                                                        profile.speedErrorCorrelation(),
                                                        profile.approachEfficiency(),
                                                        profile.towardRatio(),
                                                        targetDistance,
                                                        profile.intervalCount(),
                                                        profile.normalizedSpeedProfile());
    }
}
