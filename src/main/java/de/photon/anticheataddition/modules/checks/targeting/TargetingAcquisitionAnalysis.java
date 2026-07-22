package de.photon.anticheataddition.modules.checks.targeting;

import de.photon.anticheataddition.user.data.subdata.TargetingData;

import java.util.Arrays;

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
    private static final double RAY_EPSILON = 1E-9D;

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

        final TargetPoint targetPoint = targetPointForFinalLook(x[length - 1],
                                                                y[length - 1] + eyeHeight,
                                                                z[length - 1],
                                                                yaw[length - 1],
                                                                pitch[length - 1],
                                                                targetBox);
        if (targetPoint == null) return Result.invalid(InvalidReason.FINAL_DIRECTION_TOO_FAR);

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
            final double currentSpeed = angularRotationDistance(yaw[previous],
                                                                pitch[previous],
                                                                yaw[current],
                                                                pitch[current]);
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
        final double[] usedGain = Arrays.copyOf(gain, gainCount);
        final double meanGain = mean(usedGain);
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

    private static TargetPoint targetPointForFinalLook(final double eyeX,
                                                       final double eyeY,
                                                       final double eyeZ,
                                                       final double yaw,
                                                       final double pitch,
                                                       final TargetBox targetBox)
    {
        final Direction direction = direction(yaw, pitch);
        final double intersectionDistance = rayIntersectionDistance(eyeX,
                                                                    eyeY,
                                                                    eyeZ,
                                                                    direction.x(),
                                                                    direction.y(),
                                                                    direction.z(),
                                                                    targetBox);
        if (Double.isFinite(intersectionDistance)) {
            return new TargetPoint(eyeX + direction.x() * intersectionDistance,
                                   eyeY + direction.y() * intersectionDistance,
                                   eyeZ + direction.z() * intersectionDistance);
        }

        double minimumAngle = 180D;
        TargetPoint closest = null;
        for (int xIndex = 0; xIndex < 3; xIndex++) {
            final double pointX = targetBox.coordinateX(xIndex);
            for (int yIndex = 0; yIndex < 3; yIndex++) {
                final double pointY = targetBox.coordinateY(yIndex);
                for (int zIndex = 0; zIndex < 3; zIndex++) {
                    final double pointZ = targetBox.coordinateZ(zIndex);
                    final TargetPoint candidate = new TargetPoint(pointX, pointY, pointZ);
                    final double angle = angularErrorToPoint(eyeX, eyeY, eyeZ, yaw, pitch, candidate);
                    if (angle < minimumAngle) {
                        minimumAngle = angle;
                        closest = candidate;
                    }
                }
            }
        }
        return minimumAngle <= MAXIMUM_FINAL_ERROR ? closest : null;
    }

    private static double angularErrorToPoint(final double eyeX,
                                              final double eyeY,
                                              final double eyeZ,
                                              final double yaw,
                                              final double pitch,
                                              final TargetPoint targetPoint)
    {
        final Direction direction = direction(yaw, pitch);
        final double offsetX = targetPoint.x() - eyeX;
        final double offsetY = targetPoint.y() - eyeY;
        final double offsetZ = targetPoint.z() - eyeZ;
        final double length = Math.sqrt(offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ);
        if (length <= RAY_EPSILON) return 0D;
        final double dot = Math.clamp((direction.x() * offsetX +
                                       direction.y() * offsetY +
                                       direction.z() * offsetZ) / length,
                                      -1D,
                                      1D);
        return Math.toDegrees(Math.acos(dot));
    }

    private static Direction direction(final double yaw, final double pitch)
    {
        final double yawRadians = Math.toRadians(yaw);
        final double pitchRadians = Math.toRadians(pitch);
        final double cosPitch = Math.cos(pitchRadians);
        return new Direction(-cosPitch * Math.sin(yawRadians),
                             -Math.sin(pitchRadians),
                             cosPitch * Math.cos(yawRadians));
    }

    /**
     * Returns the smallest angular error between the supplied look ray and a conservative axis-aligned target box.
     */
    public static double angularErrorToBox(final double eyeX,
                                           final double eyeY,
                                           final double eyeZ,
                                           final double yaw,
                                           final double pitch,
                                           final TargetBox targetBox)
    {
        final Direction direction = direction(yaw, pitch);
        final double directionX = direction.x();
        final double directionY = direction.y();
        final double directionZ = direction.z();

        if (rayIntersectsBox(eyeX,
                             eyeY,
                             eyeZ,
                             directionX,
                             directionY,
                             directionZ,
                             targetBox)) return 0D;

        double minimumAngle = 180D;
        for (int xIndex = 0; xIndex < 3; xIndex++) {
            final double pointX = targetBox.coordinateX(xIndex);
            for (int yIndex = 0; yIndex < 3; yIndex++) {
                final double pointY = targetBox.coordinateY(yIndex);
                for (int zIndex = 0; zIndex < 3; zIndex++) {
                    final double pointZ = targetBox.coordinateZ(zIndex);
                    final double offsetX = pointX - eyeX;
                    final double offsetY = pointY - eyeY;
                    final double offsetZ = pointZ - eyeZ;
                    final double length = Math.sqrt(offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ);
                    if (length <= RAY_EPSILON) return 0D;

                    final double dot = Math.clamp((directionX * offsetX +
                                                   directionY * offsetY +
                                                   directionZ * offsetZ) / length,
                                                  -1D,
                                                  1D);
                    minimumAngle = Math.min(minimumAngle, Math.toDegrees(Math.acos(dot)));
                }
            }
        }
        return minimumAngle;
    }

    private static boolean rayIntersectsBox(final double originX,
                                            final double originY,
                                            final double originZ,
                                            final double directionX,
                                            final double directionY,
                                            final double directionZ,
                                            final TargetBox box)
    {
        return Double.isFinite(rayIntersectionDistance(originX,
                                                       originY,
                                                       originZ,
                                                       directionX,
                                                       directionY,
                                                       directionZ,
                                                       box));
    }

    private static double rayIntersectionDistance(final double originX,
                                                  final double originY,
                                                  final double originZ,
                                                  final double directionX,
                                                  final double directionY,
                                                  final double directionZ,
                                                  final TargetBox box)
    {
        double minimumT = 0D;
        double maximumT = Double.POSITIVE_INFINITY;

        final double[] origin = {originX, originY, originZ};
        final double[] direction = {directionX, directionY, directionZ};
        final double[] minimum = {box.minimumX(), box.minimumY(), box.minimumZ()};
        final double[] maximum = {box.maximumX(), box.maximumY(), box.maximumZ()};

        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(direction[axis]) <= RAY_EPSILON) {
                if (origin[axis] < minimum[axis] || origin[axis] > maximum[axis]) {
                    return Double.POSITIVE_INFINITY;
                }
                continue;
            }

            double first = (minimum[axis] - origin[axis]) / direction[axis];
            double second = (maximum[axis] - origin[axis]) / direction[axis];
            if (first > second) {
                final double temporary = first;
                first = second;
                second = temporary;
            }
            minimumT = Math.max(minimumT, first);
            maximumT = Math.min(maximumT, second);
            if (maximumT < minimumT) return Double.POSITIVE_INFINITY;
        }
        return maximumT >= 0D ? minimumT : Double.POSITIVE_INFINITY;
    }


    private static double angularRotationDistance(final double firstYaw,
                                                  final double firstPitch,
                                                  final double secondYaw,
                                                  final double secondPitch)
    {
        final double firstYawRadians = Math.toRadians(firstYaw);
        final double firstPitchRadians = Math.toRadians(firstPitch);
        final double secondYawRadians = Math.toRadians(secondYaw);
        final double secondPitchRadians = Math.toRadians(secondPitch);

        final double firstCosPitch = Math.cos(firstPitchRadians);
        final double secondCosPitch = Math.cos(secondPitchRadians);
        final double dot = Math.clamp(firstCosPitch * secondCosPitch * Math.cos(secondYawRadians - firstYawRadians) +
                                      Math.sin(firstPitchRadians) * Math.sin(secondPitchRadians),
                                      -1D,
                                      1D);
        return Math.toDegrees(Math.acos(dot));
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

    private static double distanceToBoxCenter(final double x,
                                              final double y,
                                              final double z,
                                              final TargetBox targetBox)
    {
        final double deltaX = targetBox.centerX() - x;
        final double deltaY = targetBox.centerY() - y;
        final double deltaZ = targetBox.centerZ() - z;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    private static double median(final double[] values)
    {
        if (values.length == 0) return Double.NaN;
        final double[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        final int middle = sorted.length / 2;
        return (sorted.length & 1) == 0 ? (sorted[middle - 1] + sorted[middle]) * 0.5D : sorted[middle];
    }

    private static double mean(final double[] values)
    {
        if (values.length == 0) return Double.NaN;
        double sum = 0D;
        for (double value : values) sum += value;
        return sum / values.length;
    }

    private static double coefficientOfVariation(final double[] values)
    {
        if (values.length < 2) return Double.NaN;
        final double mean = mean(values);
        if (!Double.isFinite(mean) || Math.abs(mean) <= 1E-9D) return Double.NaN;
        double squareSum = 0D;
        for (double value : values) {
            final double difference = value - mean;
            squareSum += difference * difference;
        }
        return Math.sqrt(squareSum / (values.length - 1D)) / Math.abs(mean);
    }

    private static double correlation(final double[] first, final double[] second)
    {
        if (first.length != second.length || first.length < 2) return 0D;
        final double firstMean = mean(first);
        final double secondMean = mean(second);
        double covariance = 0D;
        double firstSquareSum = 0D;
        double secondSquareSum = 0D;
        for (int i = 0; i < first.length; i++) {
            final double firstDifference = first[i] - firstMean;
            final double secondDifference = second[i] - secondMean;
            covariance += firstDifference * secondDifference;
            firstSquareSum += firstDifference * firstDifference;
            secondSquareSum += secondDifference * secondDifference;
        }
        final double denominator = Math.sqrt(firstSquareSum * secondSquareSum);
        return denominator <= 1E-12D ? 0D : covariance / denominator;
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

        private double coordinateX(final int index)
        {
            return coordinate(minimumX, maximumX, index);
        }

        private double coordinateY(final int index)
        {
            return coordinate(minimumY, maximumY, index);
        }

        private double coordinateZ(final int index)
        {
            return coordinate(minimumZ, maximumZ, index);
        }

        private static double coordinate(final double minimum, final double maximum, final int index)
        {
            return switch (index) {
                case 0 -> minimum;
                case 1 -> (minimum + maximum) * 0.5D;
                case 2 -> maximum;
                default -> throw new IllegalArgumentException("index must be between 0 and 2");
            };
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

    private record TargetPoint(double x, double y, double z)
    {
    }

    private record Direction(double x, double y, double z)
    {
    }

    private record Candidate(Profile profile, double quality)
    {
    }

    private record Activation(double error, double dropRatio)
    {
    }
}
