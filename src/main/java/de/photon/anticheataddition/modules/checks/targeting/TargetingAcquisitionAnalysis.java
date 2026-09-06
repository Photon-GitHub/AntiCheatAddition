package de.photon.anticheataddition.modules.checks.targeting;

import de.photon.anticheataddition.modules.checks.targeting.TargetingGeometry.TargetPoint;
import de.photon.anticheataddition.user.data.subdata.TargetingData;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import java.util.Arrays;

import static de.photon.anticheataddition.modules.checks.targeting.TargetingGeometry.*;
import static de.photon.anticheataddition.util.mathematics.DataUtil.*;

/**
 * Target-relative analysis for gradual aim-assist slowdowns.
 *
 * <p>The analysis does not flag a player merely for decelerating near a target. That is ordinary aiming. It extracts a
 * normalized acquisition profile only when the player started clearly outside a conservatively expanded target box,
 * made sustained progress toward it, and ended on or very close to it. Repetition across several independent
 * acquisitions is handled by {@code TargetingAcquisitionData}.</p>
 */
public final class TargetingAcquisitionAnalysis
{
    public static final int PROFILE_BIN_COUNT = 5;
    public static final int MINIMUM_SAMPLE_COUNT = 8;

    private static final double MINIMUM_INITIAL_ERROR = 2.5D;
    private static final double MAXIMUM_INITIAL_ERROR = 35D;
    private static final double MAXIMUM_FINAL_ERROR = 0.65D;
    private static final double MINIMUM_ROTATION_SPEED = 0.015D;
    private static final double MINIMUM_TOTAL_PROGRESS = 2D;
    private static final double MINIMUM_APPROACH_EFFICIENCY = 0.60D;
    private static final double MINIMUM_TOWARD_RATIO = 0.62D;
    private static final double MAXIMUM_CANDIDATE_SLOWDOWN_RATIO = 0.68D;
    private static final double MINIMUM_FAR_RATIO = 0.55D;
    private static final double MAXIMUM_NEAR_RATIO = 0.30D;
    private static final double MINIMUM_ACTIVATION_DROP = 0.20D;
    private static final double MINIMUM_ACTIVATION_ERROR = 0.75D;
    private static final double MAXIMUM_ACTIVATION_ERROR = 8D;

    private TargetingAcquisitionAnalysis()
    {
    }

    /**
     * Extracts the strongest valid approach ending at a successful player hit.
     *
     * @param snapshot  packet-ordered attacker position and rotation history
     * @param targetBox conservative target box in world coordinates
     * @param eyeHeight current attacker eye height; only stable ground-based acquisitions should be passed here
     * @return an invalid result when the geometry or movement does not provide a safe acquisition sample
     */
    public static Result analyze(final TargetingData.AcquisitionSnapshot snapshot,
                                 final TargetBox targetBox,
                                 final double eyeHeight)
    {
        if (snapshot == null || targetBox == null || !Double.isFinite(eyeHeight) || eyeHeight <= 0D) {
            return Result.invalid(InvalidReason.INVALID_INPUT);
        }

        final double[] x = snapshot.x();
        final double[] y = snapshot.y();
        final double[] z = snapshot.z();
        final double[] yaw = snapshot.yaw();
        final double[] pitch = snapshot.pitch();
        if (yaw.length < MINIMUM_SAMPLE_COUNT) return Result.invalid(InvalidReason.NOT_ENOUGH_SAMPLES);

        final int length = yaw.length;
        for (int i = 0; i < length; i++) {
            if (!allFinite(x[i], y[i], z[i], yaw[i], pitch[i])) return Result.invalid(InvalidReason.INVALID_INPUT);
        }

        final TargetPoint targetPoint = closestPointForLook(x[length - 1], y[length - 1] + eyeHeight,
                                                            z[length - 1], yaw[length - 1], pitch[length - 1],
                                                            targetBox);
        if (targetPoint == null || angularErrorToPoint(x[length - 1], y[length - 1] + eyeHeight,
                                                       z[length - 1], yaw[length - 1], pitch[length - 1],
                                                       targetPoint) > MAXIMUM_FINAL_ERROR) {
            return Result.invalid(InvalidReason.FINAL_DIRECTION_TOO_FAR);
        }

        final double[] error = new double[length];
        for (int i = 0; i < length; i++) {
            error[i] = angularErrorToPoint(x[i],
                                           y[i] + eyeHeight,
                                           z[i],
                                           yaw[i],
                                           pitch[i],
                                           targetPoint);
        }

        Candidate best = null;
        for (int start = 0; start <= length - MINIMUM_SAMPLE_COUNT; start++) {
            final double initialError = error[start];
            if (initialError < MINIMUM_INITIAL_ERROR || initialError > MAXIMUM_INITIAL_ERROR) continue;

            // Movement within the target hitbox is tracking, not a new acquisition.
            if (angularErrorToBox(x[start], y[start] + eyeHeight, z[start], yaw[start], pitch[start], targetBox)
                < MINIMUM_INITIAL_ERROR) continue;

            final Candidate candidate = evaluateCandidate(start, yaw, pitch, error, targetBox, x, y, z);
            if (candidate == null) continue;
            if (best == null || candidate.quality() > best.quality()) best = candidate;
        }

        if (best == null) return Result.invalid(InvalidReason.NO_RELIABLE_APPROACH);
        return new Result(true, InvalidReason.NONE, best.profile());
    }

    private static Candidate evaluateCandidate(final int start,
                                               final double[] yaw,
                                               final double[] pitch,
                                               final double[] error,
                                               final TargetBox targetBox,
                                               final double[] x,
                                               final double[] y,
                                               final double[] z)
    {
        final int end = error.length - 1;
        final int intervalCount = end - start;
        if (intervalCount < MINIMUM_SAMPLE_COUNT - 1) return null;

        final double initialError = error[start];
        final double finalError = error[end];
        final double totalProgress = initialError - finalError;
        if (totalProgress < MINIMUM_TOTAL_PROGRESS) return null;

        final Approach approach = measureApproach(start, yaw, pitch, error);
        if (approach == null) return null;
        final double[] speed = approach.speed();
        final double[] precedingError = approach.precedingError();
        final double[] errorRatio = approach.errorRatio();
        final double towardRatio = approach.towardRatio();
        final double approachEfficiency = approach.efficiency();

        final double[] farSpeed = selectSpeeds(speed, errorRatio, MINIMUM_FAR_RATIO, 1D);
        final double[] nearSpeed = selectSpeeds(speed, errorRatio, 0D, MAXIMUM_NEAR_RATIO);
        if (farSpeed.length < 2 || nearSpeed.length < 2) return null;

        final double farMedian = median(farSpeed);
        final double nearMedian = median(nearSpeed);
        if (farMedian < MINIMUM_ROTATION_SPEED) return null;
        final double slowdownRatio = nearMedian / farMedian;

        final double[] normalizedProfile = createNormalizedProfile(speed, errorRatio, farMedian);
        int populatedBins = 0;
        for (double value : normalizedProfile) {
            if (Double.isFinite(value)) populatedBins++;
        }
        if (populatedBins < PROFILE_BIN_COUNT - 1) return null;
        fillMissingBins(normalizedProfile);

        final Activation activation = findActivation(speed, precedingError);
        final double[] usedGain = approach.gain();
        final double meanGain = usedGain.length == 0 ? Double.NaN : average(usedGain);
        final double gainVariation = coefficientOfVariation(usedGain);
        final double speedErrorCorrelation = correlation(speed, precedingError);
        final int nonIncreasingTransitions = nonIncreasingTransitions(normalizedProfile);

        int candidateScore = 0;
        if (slowdownRatio <= MAXIMUM_CANDIDATE_SLOWDOWN_RATIO) candidateScore++;
        if (approachEfficiency >= 0.72D) candidateScore++;
        if (towardRatio >= 0.72D) candidateScore++;
        if (speedErrorCorrelation >= 0.72D) candidateScore++;
        if (Double.isFinite(gainVariation) && gainVariation <= 0.60D) candidateScore++;
        if (nonIncreasingTransitions >= PROFILE_BIN_COUNT - 2) candidateScore++;
        final boolean targetBoundActivation = Double.isFinite(activation.error()) &&
                                              activation.error() >= MINIMUM_ACTIVATION_ERROR &&
                                              activation.error() <= MAXIMUM_ACTIVATION_ERROR &&
                                              activation.dropRatio() >= MINIMUM_ACTIVATION_DROP;
        if (targetBoundActivation) candidateScore++;

        final boolean controlledSlowdownCandidate = slowdownRatio <= MAXIMUM_CANDIDATE_SLOWDOWN_RATIO &&
                                                    targetBoundActivation &&
                                                    candidateScore >= 4;
        final double targetDistance = distanceToBoxCenter(x[end], y[end], z[end], targetBox);
        final Profile profile = new Profile(controlledSlowdownCandidate,
                                            initialError,
                                            finalError,
                                            slowdownRatio,
                                            activation.error(),
                                            activation.dropRatio(),
                                            meanGain,
                                            gainVariation,
                                            speedErrorCorrelation,
                                            approachEfficiency,
                                            towardRatio,
                                            targetDistance,
                                            intervalCount,
                                            normalizedProfile);
        final double quality = totalProgress * approachEfficiency * towardRatio * Math.max(0.25D, 1D - finalError);
        return new Candidate(profile, quality);
    }

    /** Measures motion and rejects approaches without enough sustained progress before any slowdown scoring. */
    private static Approach measureApproach(final int start, final double[] yaw, final double[] pitch,
                                            final double[] error)
    {
        final int intervalCount = error.length - 1 - start;
        final double initialError = error[start];
        final double totalProgress = initialError - error[error.length - 1];
        final double[] speed = new double[intervalCount];
        final double[] precedingError = new double[intervalCount];
        final double[] gain = new double[intervalCount];
        final double[] errorRatio = new double[intervalCount];
        int movingCount = 0;
        int towardCount = 0;
        int gainCount = 0;
        double absoluteErrorTravel = 0D;

        for (int interval = 0; interval < intervalCount; interval++) {
            final int previous = start + interval;
            final int current = previous + 1;
            final double currentSpeed = MathUtil.getAngleBetweenRotations(yaw[previous], pitch[previous],
                                                                          yaw[current], pitch[current]);
            final double progress = error[previous] - error[current];
            speed[interval] = currentSpeed;
            precedingError[interval] = error[previous];
            errorRatio[interval] = Math.clamp(error[previous] / initialError, 0D, 1D);
            absoluteErrorTravel += Math.abs(progress);

            if (currentSpeed >= MINIMUM_ROTATION_SPEED) {
                movingCount++;
                if (progress > Math.max(0.01D, currentSpeed * 0.08D)) towardCount++;
            }
            if (progress > 0.01D && error[previous] > MAXIMUM_FINAL_ERROR) {
                gain[gainCount++] = progress / error[previous];
            }
        }

        if (movingCount < MINIMUM_SAMPLE_COUNT - 2) return null;
        final double towardRatio = towardCount / (double) movingCount;
        final double approachEfficiency = absoluteErrorTravel == 0D ? 0D : totalProgress / absoluteErrorTravel;
        if (towardRatio < MINIMUM_TOWARD_RATIO || approachEfficiency < MINIMUM_APPROACH_EFFICIENCY) return null;

        return new Approach(speed, precedingError, errorRatio, Arrays.copyOf(gain, gainCount),
                            towardRatio, approachEfficiency);
    }

    private record Approach(double[] speed, double[] precedingError, double[] errorRatio, double[] gain,
                            double towardRatio, double efficiency) {}

    /**
     * Returns the smallest angular error between the supplied look ray and a conservative axis-aligned target box.
     */
    public static double angularErrorToBox(final double eyeX, final double eyeY, final double eyeZ,
                                           final double yaw, final double pitch, final TargetBox targetBox)
    {
        if (targetBox == null || !allFinite(eyeX, eyeY, eyeZ, yaw, pitch)) return Double.NaN;
        return TargetingGeometry.angularErrorToBox(eyeX, eyeY, eyeZ, yaw, pitch, targetBox);
    }

    private static double[] selectSpeeds(final double[] speed,
                                         final double[] errorRatio,
                                         final double minimumRatio,
                                         final double maximumRatio)
    {
        final double[] selected = new double[speed.length];
        int count = 0;
        for (int i = 0; i < speed.length; i++) {
            if (errorRatio[i] >= minimumRatio && errorRatio[i] <= maximumRatio && speed[i] >= MINIMUM_ROTATION_SPEED) {
                selected[count++] = speed[i];
            }
        }
        return Arrays.copyOf(selected, count);
    }

    private static double[] createNormalizedProfile(final double[] speed,
                                                    final double[] errorRatio,
                                                    final double normalization)
    {
        final double[] result = new double[PROFILE_BIN_COUNT];
        Arrays.fill(result, Double.NaN);
        final double[][] values = new double[PROFILE_BIN_COUNT][speed.length];
        final int[] count = new int[PROFILE_BIN_COUNT];

        for (int i = 0; i < speed.length; i++) {
            if (speed[i] < MINIMUM_ROTATION_SPEED) continue;
            final int bin = Math.min(PROFILE_BIN_COUNT - 1,
                                     (int) Math.floor((1D - Math.clamp(errorRatio[i], 0D, 1D)) * PROFILE_BIN_COUNT));
            values[bin][count[bin]++] = speed[i] / normalization;
        }

        for (int bin = 0; bin < PROFILE_BIN_COUNT; bin++) {
            if (count[bin] > 0) result[bin] = median(Arrays.copyOf(values[bin], count[bin]));
        }
        return result;
    }

    private static void fillMissingBins(final double[] profile)
    {
        for (int i = 0; i < profile.length; i++) {
            if (Double.isFinite(profile[i])) continue;
            int lower = i - 1;
            while (lower >= 0 && !Double.isFinite(profile[lower])) lower--;
            int upper = i + 1;
            while (upper < profile.length && !Double.isFinite(profile[upper])) upper++;

            if (lower >= 0 && upper < profile.length) {
                final double position = (i - lower) / (double) (upper - lower);
                profile[i] = profile[lower] + (profile[upper] - profile[lower]) * position;
            } else if (lower >= 0) {
                profile[i] = profile[lower];
            } else if (upper < profile.length) {
                profile[i] = profile[upper];
            } else {
                profile[i] = 1D;
            }
        }
    }

    private static Activation findActivation(final double[] speed, final double[] precedingError)
    {
        double strongestDrop = 0D;
        double activationError = Double.NaN;
        // The final two intervals are deliberately excluded. A successful hit defines the eventual aim point, so
        // every legitimate acquisition naturally ends with a small final correction which must not masquerade as an
        // assist activation boundary.
        for (int i = 2; i < speed.length - 2; i++) {
            if (precedingError[i] < MINIMUM_ACTIVATION_ERROR ||
                precedingError[i] > MAXIMUM_ACTIVATION_ERROR) continue;
            final double baseline = (speed[i - 1] + speed[i - 2]) * 0.5D;
            if (baseline < MINIMUM_ROTATION_SPEED || speed[i] >= baseline) continue;
            final double drop = 1D - speed[i] / baseline;
            if (drop > strongestDrop) {
                strongestDrop = drop;
                activationError = precedingError[i];
            }
        }
        return new Activation(activationError, strongestDrop);
    }

    private static int nonIncreasingTransitions(final double[] profile)
    {
        int count = 0;
        for (int i = 1; i < profile.length; i++) {
            if (profile[i] <= profile[i - 1] * 1.12D) count++;
        }
        return count;
    }

    private static boolean allFinite(final double... values)
    {
        for (double value : values) {
            if (!Double.isFinite(value)) return false;
        }
        return true;
    }

    /**
     * Conservative axis-aligned target box used only by the pure target-relative analysis.
     */
    public record TargetBox(double minimumX,
                            double minimumY,
                            double minimumZ,
                            double maximumX,
                            double maximumY,
                            double maximumZ)
    {
        public TargetBox
        {
            if (!allFinite(minimumX, minimumY, minimumZ, maximumX, maximumY, maximumZ) ||
                minimumX > maximumX || minimumY > maximumY || minimumZ > maximumZ) {
                throw new IllegalArgumentException("invalid target box");
            }
        }

        public double centerX()
        {
            return (minimumX + maximumX) * 0.5D;
        }

        public double centerY()
        {
            return (minimumY + maximumY) * 0.5D;
        }

        public double centerZ()
        {
            return (minimumZ + maximumZ) * 0.5D;
        }

    }

    /**
     * One valid, target-relative acquisition profile. It is not itself a violation.
     */
    public record Profile(boolean controlledSlowdownCandidate,
                          double initialError,
                          double finalError,
                          double slowdownRatio,
                          double activationError,
                          double activationDrop,
                          double meanGain,
                          double gainVariation,
                          double speedErrorCorrelation,
                          double approachEfficiency,
                          double towardRatio,
                          double targetDistance,
                          int intervalCount,
                          double[] normalizedSpeedProfile)
    {
        public Profile
        {
            normalizedSpeedProfile = Arrays.copyOf(normalizedSpeedProfile, normalizedSpeedProfile.length);
            if (normalizedSpeedProfile.length != PROFILE_BIN_COUNT) {
                throw new IllegalArgumentException("normalizedSpeedProfile must contain " + PROFILE_BIN_COUNT + " bins");
            }
        }

        @Override
        public double[] normalizedSpeedProfile()
        {
            return Arrays.copyOf(normalizedSpeedProfile, normalizedSpeedProfile.length);
        }
    }

    /**
     * Result of one successful-hit acquisition analysis.
     */
    public record Result(boolean valid, InvalidReason invalidReason, Profile profile)
    {
        public static Result invalid(final InvalidReason reason)
        {
            return new Result(false, reason, null);
        }
    }

    /**
     * Diagnostic reason why a successful hit could not safely be used as an acquisition sample.
     */
    public enum InvalidReason
    {
        NONE,
        INVALID_INPUT,
        NOT_ENOUGH_SAMPLES,
        FINAL_DIRECTION_TOO_FAR,
        NO_RELIABLE_APPROACH
    }

    private record Candidate(Profile profile, double quality)
    {
    }

    private record Activation(double error, double dropRatio)
    {
    }
}
