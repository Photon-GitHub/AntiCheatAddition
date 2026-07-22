package de.photon.anticheataddition.modules.checks.targeting;

import de.photon.anticheataddition.util.mathematics.KolmogorovSmirnov;

import java.util.Arrays;

/**
 * Pure statistical analysis shared by the Targeting submodules.
 *
 * <p>The absolute yaw and pitch values mostly describe the player's intended camera movement. Each axis is therefore
 * detrended with a quadratic least-squares curve. The remaining residuals describe the small deviations around that
 * movement and are considerably more useful for distinguishing human corrections from generated targeting offsets.</p>
 *
 * <p>Three overlapping windows are inspected: the complete sample, its oldest 32 samples, and its newest 32 samples.
 * This makes the analysis less vulnerable to clients which add a short clean section to poison an otherwise suspicious
 * window or alternate their behavior between yaw and pitch. Precision is intentionally evaluated only on the complete
 * window because a short naturally smooth section is not strong evidence by itself.</p>
 */
public final class TargetingAnalysis
{
    public static final int MINIMUM_SAMPLE_COUNT = 32;

    private static final double PRECISE_MAX_STANDARD_DEVIATION = 0.0005D;
    private static final double PRECISE_MAX_ABSOLUTE_RESIDUAL = 0.0015D;

    private static final double RANDOM_MINIMUM_SCORE = 0.78D;
    private static final double DISTRIBUTION_FREE_MINIMUM_SCORE = 0.68D;
    private static final double RANDOM_LAG_ONE_SCALE = 0.6D;
    private static final double RANDOM_AVERAGE_AUTOCORRELATION_SCALE = 0.45D;
    private static final double RANDOM_VARIANCE_LOG_SCALE = 2.5D;
    private static final double RANDOM_RUNS_Z_SCALE = 3.5D;

    private static final double PITCH_CLAMP_EPSILON = 0.0001D;
    private static final double PITCH_CLAMP_MINIMUM_RATIO = 0.75D;

    private static final double YAW_MINIMUM_DISCONTINUITY = 25D;
    private static final double PITCH_MINIMUM_DISCONTINUITY = 15D;
    private static final double DISCONTINUITY_MAD_MULTIPLIER = 10D;

    private static final double UNIFORM_MIN_P_VALUE = 0.1D;
    private static final double UNIFORM_MAX_D_STATISTIC = 0.22D;
    private static final double GAUSSIAN_MAX_ABS_SKEWNESS = 0.9D;
    private static final double GAUSSIAN_MIN_KURTOSIS = 1.7D;
    private static final double GAUSSIAN_MAX_KURTOSIS = 5D;

    private static final double PERIODIC_MIN_CORRELATION = 0.9D;
    private static final double PERIODIC_MAX_REPEAT_ERROR = 0.35D;
    private static final double STRONG_FULL_PERIODIC_MIN_CORRELATION = 0.97D;
    private static final double STRONG_FULL_PERIODIC_MAX_REPEAT_ERROR = 0.28D;
    private static final double ALTERNATING_MAX_LAG_ONE = -0.72D;
    private static final double ALTERNATING_MIN_LAG_TWO = 0.72D;
    private static final double ALTERNATING_MIN_SIGN_CHANGE_RATIO = 0.8D;
    private static final double LOW_ENTROPY_MAX_PERMUTATION_ENTROPY = 0.55D;
    private static final double LOW_ENTROPY_MAX_DISTINCT_LEVEL_RATIO = 0.32D;
    private static final double LOW_ENTROPY_MIN_PERIODIC_CORRELATION = 0.65D;

    private TargetingAnalysis()
    {
    }

    /**
     * Analyzes yaw and pitch independently and returns classifications and supporting metrics.
     *
     * @param yaw   absolute yaw samples in chronological order
     * @param pitch absolute pitch samples in chronological order
     * @return the combined multi-window result
     */
    public static Result analyze(final double[] yaw, final double[] pitch)
    {
        if (yaw == null || pitch == null) throw new NullPointerException("yaw and pitch must not be null");
        return analyze(yaw, pitch, new boolean[yaw.length]);
    }

    /**
     * Analyzes yaw and pitch while treating marked server-authoritative transitions as segment boundaries.
     *
     * @param trustedBreakBefore {@code true} at a sample whose transition from the preceding sample was caused by the
     *                           server, for example a teleport
     */
    public static Result analyze(final double[] yaw,
                                 final double[] pitch,
                                 final boolean[] trustedBreakBefore)
    {
        if (yaw == null || pitch == null || trustedBreakBefore == null) {
            throw new NullPointerException("yaw, pitch and trustedBreakBefore must not be null");
        }
        if (yaw.length != pitch.length || yaw.length != trustedBreakBefore.length) {
            throw new IllegalArgumentException("all sample arrays must contain the same number of entries");
        }
        if (yaw.length < MINIMUM_SAMPLE_COUNT) throw new IllegalArgumentException("not enough rotation samples");

        validateFinite(yaw);
        validateFinite(pitch);

        final double[] unwrappedYaw = unwrapYaw(yaw);
        final double[] copiedPitch = Arrays.copyOf(pitch, pitch.length);
        final boolean[] copiedBreaks = Arrays.copyOf(trustedBreakBefore, trustedBreakBefore.length);
        final AxisAnalysis fullYaw = analyzeAxis(unwrappedYaw, YAW_MINIMUM_DISCONTINUITY, copiedBreaks);
        final AxisAnalysis fullPitch = analyzePitchAxis(copiedPitch, copiedBreaks);

        final int subWindowLength = MINIMUM_SAMPLE_COUNT;
        final boolean[] earlyBreaks = Arrays.copyOfRange(copiedBreaks, 0, subWindowLength);
        final AxisAnalysis earlyYaw = analyzeAxis(Arrays.copyOfRange(unwrappedYaw, 0, subWindowLength),
                                                  YAW_MINIMUM_DISCONTINUITY,
                                                  earlyBreaks);
        final AxisAnalysis earlyPitch = analyzePitchAxis(Arrays.copyOfRange(copiedPitch, 0, subWindowLength),
                                                         earlyBreaks);
        final int recentStart = yaw.length - subWindowLength;
        final boolean[] recentBreaks = Arrays.copyOfRange(copiedBreaks, recentStart, copiedBreaks.length);
        recentBreaks[0] = false;
        final AxisAnalysis recentYaw = analyzeAxis(Arrays.copyOfRange(unwrappedYaw, recentStart, yaw.length),
                                                   YAW_MINIMUM_DISCONTINUITY,
                                                   recentBreaks);
        final AxisAnalysis recentPitch = analyzePitchAxis(Arrays.copyOfRange(copiedPitch, recentStart, pitch.length),
                                                          recentBreaks);

        final AxisResult yawResult = combineAxis(fullYaw.result(), earlyYaw.result(), recentYaw.result());
        final AxisResult pitchResult = combineAxis(fullPitch.result(), earlyPitch.result(), recentPitch.result());
        final double crossCorrelation = correlation(fullYaw.residuals(), fullPitch.residuals(), 0);

        return new Result(yawResult,
                          pitchResult,
                          crossCorrelation,
                          normalizeFingerprint(fullYaw.residuals(), fullYaw.result()),
                          normalizeFingerprint(fullPitch.residuals(), fullPitch.result()));
    }

    /**
     * Calculates the shortest signed yaw delta in the range [-180, 180].
     */
    public static double signedYawDelta(final double currentYaw, final double previousYaw)
    {
        final double normalizedCurrent = normalizeYaw(currentYaw);
        final double normalizedPrevious = normalizeYaw(previousYaw);
        if (!Double.isFinite(normalizedCurrent) || !Double.isFinite(normalizedPrevious)) return Double.NaN;

        double delta = normalizedCurrent - normalizedPrevious;
        if (delta > 180D) delta -= 360D;
        else if (delta < -180D) delta += 360D;
        return delta;
    }

    /**
     * Canonicalizes any finite yaw without subtracting the potentially very large raw input values.
     *
     * @return a yaw in [-180, 180), or NaN for a non-finite input
     */
    public static double normalizeYaw(final double yaw)
    {
        if (!Double.isFinite(yaw)) return Double.NaN;
        double normalized = yaw % 360D;
        if (normalized >= 180D) normalized -= 360D;
        else if (normalized < -180D) normalized += 360D;
        return normalized == -0D ? 0D : normalized;
    }

    private static AxisResult combineAxis(final AxisResult full,
                                          final AxisResult early,
                                          final AxisResult recent)
    {
        AxisResult randomRepresentative = null;
        AxisResult syntheticRepresentative = null;
        int randomWindowCount = 0;
        int syntheticWindowCount = 0;

        final AxisResult[] windows = {full, early, recent};
        for (AxisResult window : windows) {
            if (window.pattern().isRandomized()) {
                randomWindowCount++;
                if (randomRepresentative == null || randomConfidence(window) > randomConfidence(randomRepresentative)) {
                    randomRepresentative = window;
                }
            }
            if (window.syntheticPattern() != SyntheticPattern.NONE) {
                syntheticWindowCount++;
                if (syntheticRepresentative == null || syntheticConfidence(window) > syntheticConfidence(syntheticRepresentative)) {
                    syntheticRepresentative = window;
                }
            }
        }

        AxisResult representative = full;
        if (randomRepresentative != null) representative = randomRepresentative;
        else if (syntheticRepresentative != null) representative = syntheticRepresentative;

        // A precise classification is deliberately retained only when the complete window is precise.
        final Pattern pattern = full.pattern() == Pattern.PRECISE
                                ? Pattern.PRECISE
                                : randomRepresentative == null || randomWindowCount < 2
                                  ? Pattern.NATURAL
                                  : randomRepresentative.pattern();
        final boolean strongFullPeriodic = full.syntheticPattern() == SyntheticPattern.PERIODIC &&
                                           full.maxPeriodicCorrelation() >= STRONG_FULL_PERIODIC_MIN_CORRELATION &&
                                           full.repeatError() <= STRONG_FULL_PERIODIC_MAX_REPEAT_ERROR;
        final SyntheticPattern syntheticPattern = syntheticRepresentative == null ||
                                                  syntheticWindowCount < 2 && !strongFullPeriodic
                                                  ? SyntheticPattern.NONE
                                                  : syntheticRepresentative.syntheticPattern();

        return representative.withClassifications(pattern,
                                                  syntheticPattern,
                                                  randomWindowCount,
                                                  syntheticWindowCount,
                                                  full.standardDeviation(),
                                                  full.maxAbsoluteResidual(),
                                                  full.rotationRange());
    }

    private static double randomConfidence(final AxisResult result)
    {
        return result.randomnessScore() + result.permutationEntropy() * 0.1D;
    }

    private static double syntheticConfidence(final AxisResult result)
    {
        return result.maxPeriodicCorrelation() - result.repeatError() * 0.25D +
               (1D - result.permutationEntropy()) * 0.1D;
    }


    /**
     * Pitch is hard-clamped by the vanilla client at straight up and straight down. A long clamped section can be
     * mathematically precise without representing automated targeting, so only the precision classification is
     * suppressed. Other residual and deterministic classifications remain available.
     */
    private static AxisAnalysis analyzePitchAxis(final double[] rotations, final boolean[] trustedBreakBefore)
    {
        final AxisAnalysis analysis = analyzeAxis(rotations, PITCH_MINIMUM_DISCONTINUITY, trustedBreakBefore);
        if (analysis.result().pattern() != Pattern.PRECISE || !isPitchClampWindow(rotations)) return analysis;

        final AxisResult result = analysis.result();
        return new AxisAnalysis(AxisResult.natural(result.standardDeviation(),
                                                   result.maxAbsoluteResidual(),
                                                   result.rotationRange()),
                                analysis.residuals());
    }

    private static boolean isPitchClampWindow(final double[] rotations)
    {
        int clamped = 0;
        for (double rotation : rotations) {
            if (Math.abs(Math.abs(rotation) - 90D) <= PITCH_CLAMP_EPSILON) clamped++;
        }
        return clamped >= Math.ceil(rotations.length * PITCH_CLAMP_MINIMUM_RATIO);
    }

    private static AxisAnalysis analyzeAxis(final double[] rotations,
                                            final double minimumDiscontinuity,
                                            final boolean[] trustedBreakBefore)
    {
        final double rotationRange = range(rotations);
        final double[] correctedRotations = removeDiscontinuities(rotations,
                                                                  minimumDiscontinuity,
                                                                  trustedBreakBefore);
        final double[] residuals = detrendQuadratic(correctedRotations);
        center(residuals);

        final double variance = meanSquare(residuals, 0, residuals.length);
        final double standardDeviation = Math.sqrt(variance);
        final double maxAbsoluteResidual = maximumAbsolute(residuals);
        if (standardDeviation <= PRECISE_MAX_STANDARD_DEVIATION &&
            maxAbsoluteResidual <= PRECISE_MAX_ABSOLUTE_RESIDUAL) {
            return new AxisAnalysis(AxisResult.precise(standardDeviation, maxAbsoluteResidual, rotationRange), residuals);
        }

        if (!Double.isFinite(standardDeviation) || standardDeviation == 0D) {
            return new AxisAnalysis(AxisResult.natural(standardDeviation, maxAbsoluteResidual, rotationRange), residuals);
        }

        final double[] normalizedResiduals = normalizeResiduals(residuals, standardDeviation);
        double thirdMoment = 0D;
        double fourthMoment = 0D;
        for (double standardized : normalizedResiduals) {
            final double squared = standardized * standardized;
            thirdMoment += squared * standardized;
            fourthMoment += squared * squared;
        }

        final double skewness = thirdMoment / residuals.length;
        final double kurtosis = fourthMoment / residuals.length;
        final double lagOne = correlation(residuals, residuals, 1);
        final double lagTwo = correlation(residuals, residuals, 2);
        final double lagThree = correlation(residuals, residuals, 3);
        final double averageAbsAutocorrelation = (Math.abs(lagOne) + Math.abs(lagTwo) + Math.abs(lagThree)) / 3D;
        final double signChangeRatio = signChangeRatio(residuals);
        final double permutationEntropy = permutationEntropy(normalizedResiduals);
        final double runsZScore = runsZScore(normalizedResiduals);
        final double distinctLevelRatio = distinctLevelRatio(normalizedResiduals);
        final Periodicity periodicity = periodicity(normalizedResiduals);

        final int midpoint = residuals.length / 2;
        final double firstVariance = meanSquare(residuals, 0, midpoint);
        final double secondVariance = meanSquare(residuals, midpoint, residuals.length);
        final double varianceRatio = secondVariance == 0D ? Double.POSITIVE_INFINITY : firstVariance / secondVariance;

        final var ksResult = KolmogorovSmirnov.uniformTest(residuals);
        final boolean uniformLike = ksResult.pValue() >= UNIFORM_MIN_P_VALUE &&
                                    ksResult.dStatistic() <= UNIFORM_MAX_D_STATISTIC;
        final boolean gaussianLike = Math.abs(skewness) <= GAUSSIAN_MAX_ABS_SKEWNESS &&
                                     kurtosis >= GAUSSIAN_MIN_KURTOSIS &&
                                     kurtosis <= GAUSSIAN_MAX_KURTOSIS;

        // Amplitude is deliberately not used as an exemption. The temporal characteristics are combined into a
        // continuous score instead of a chain of individually bypassable cut-offs. A client therefore cannot evade
        // the check merely by moving one public metric just beyond its former threshold.
        final double randomnessScore = randomnessScore(lagOne,
                                                       averageAbsAutocorrelation,
                                                       signChangeRatio,
                                                       varianceRatio,
                                                       permutationEntropy,
                                                       runsZScore);
        final boolean randomDynamics = randomnessScore >= RANDOM_MINIMUM_SCORE;
        final double distributionFreeScore = (permutationEntropy + runsScore(runsZScore)) * 0.5D;
        final boolean distributionFreeLike = distributionFreeScore >= DISTRIBUTION_FREE_MINIMUM_SCORE;

        final Pattern pattern;
        if (!randomDynamics) pattern = Pattern.NATURAL;
        else if (uniformLike) pattern = Pattern.UNIFORM;
        else if (gaussianLike) pattern = Pattern.GAUSSIAN;
        else if (distributionFreeLike) pattern = Pattern.DISTRIBUTION_FREE;
        else pattern = Pattern.NATURAL;

        final SyntheticPattern syntheticPattern = classifySyntheticPattern(lagOne,
                                                                           lagTwo,
                                                                           signChangeRatio,
                                                                           permutationEntropy,
                                                                           distinctLevelRatio,
                                                                           periodicity);

        return new AxisAnalysis(new AxisResult(pattern,
                                               syntheticPattern,
                                               standardDeviation,
                                               maxAbsoluteResidual,
                                               rotationRange,
                                               randomnessScore,
                                               lagOne,
                                               averageAbsAutocorrelation,
                                               signChangeRatio,
                                               ksResult.dStatistic(),
                                               ksResult.pValue(),
                                               skewness,
                                               kurtosis,
                                               varianceRatio,
                                               permutationEntropy,
                                               runsZScore,
                                               distinctLevelRatio,
                                               periodicity.correlation(),
                                               periodicity.lag(),
                                               periodicity.repeatError(),
                                               pattern.isRandomized() ? 1 : 0,
                                               syntheticPattern == SyntheticPattern.NONE ? 0 : 1),
                                residuals);
    }

    private static double randomnessScore(final double lagOne,
                                          final double averageAbsAutocorrelation,
                                          final double signChangeRatio,
                                          final double varianceRatio,
                                          final double permutationEntropy,
                                          final double runsZScore)
    {
        final double lagOneScore = 1D - unitClamp(Math.abs(lagOne) / RANDOM_LAG_ONE_SCALE);
        final double averageCorrelationScore =
                1D - unitClamp(averageAbsAutocorrelation / RANDOM_AVERAGE_AUTOCORRELATION_SCALE);
        final double signScore = 1D - unitClamp(Math.abs(signChangeRatio - 0.5D) * 2D);
        final double safeVarianceRatio = Math.max(Double.MIN_NORMAL, varianceRatio);
        final double varianceScore = Math.exp(-Math.abs(Math.log(safeVarianceRatio)) / RANDOM_VARIANCE_LOG_SCALE);
        final double entropyScore = unitClamp(permutationEntropy);
        final double runsScore = runsScore(runsZScore);

        return (1.5D * lagOneScore +
                1.5D * averageCorrelationScore +
                signScore +
                varianceScore +
                1.5D * entropyScore +
                0.5D * runsScore) / 7D;
    }

    private static double runsScore(final double runsZScore)
    {
        return Double.isFinite(runsZScore)
               ? Math.exp(-Math.abs(runsZScore) / RANDOM_RUNS_Z_SCALE)
               : 0D;
    }

    private static double unitClamp(final double value)
    {
        return Math.clamp(value, 0D, 1D);
    }

    private static SyntheticPattern classifySyntheticPattern(final double lagOne,
                                                             final double lagTwo,
                                                             final double signChangeRatio,
                                                             final double permutationEntropy,
                                                             final double distinctLevelRatio,
                                                             final Periodicity periodicity)
    {
        if (lagOne <= ALTERNATING_MAX_LAG_ONE &&
            lagTwo >= ALTERNATING_MIN_LAG_TWO &&
            signChangeRatio >= ALTERNATING_MIN_SIGN_CHANGE_RATIO) return SyntheticPattern.ALTERNATING;

        if (periodicity.correlation() >= PERIODIC_MIN_CORRELATION &&
            periodicity.repeatError() <= PERIODIC_MAX_REPEAT_ERROR) return SyntheticPattern.PERIODIC;

        if (permutationEntropy <= LOW_ENTROPY_MAX_PERMUTATION_ENTROPY &&
            distinctLevelRatio <= LOW_ENTROPY_MAX_DISTINCT_LEVEL_RATIO &&
            periodicity.correlation() >= LOW_ENTROPY_MIN_PERIODIC_CORRELATION) return SyntheticPattern.LOW_ENTROPY;

        return SyntheticPattern.NONE;
    }

    /**
     * Removes isolated level shifts before detrending. The original rotations remain available to interaction checks;
     * this correction only prevents a deliberately inserted jump from poisoning all stochastic windows.
     */
    private static double[] removeDiscontinuities(final double[] values,
                                                  final double minimumDiscontinuity,
                                                  final boolean[] trustedBreakBefore)
    {
        if (values.length != trustedBreakBefore.length) {
            throw new IllegalArgumentException("values and trustedBreakBefore must have the same length");
        }
        if (values.length < 4) return Arrays.copyOf(values, values.length);

        final double[] deltas = new double[values.length - 1];
        final double[] ordinaryAbsoluteDeltas = new double[deltas.length];
        int ordinaryAbsoluteCount = 0;
        for (int i = 0; i < deltas.length; i++) {
            deltas[i] = values[i + 1] - values[i];
            if (!trustedBreakBefore[i + 1]) ordinaryAbsoluteDeltas[ordinaryAbsoluteCount++] = Math.abs(deltas[i]);
        }

        final double medianAbsoluteDelta = ordinaryAbsoluteCount == 0
                                           ? 0D
                                           : median(Arrays.copyOf(ordinaryAbsoluteDeltas, ordinaryAbsoluteCount));
        final double[] absoluteDeviations = new double[ordinaryAbsoluteCount];
        for (int i = 0; i < ordinaryAbsoluteCount; i++) {
            absoluteDeviations[i] = Math.abs(ordinaryAbsoluteDeltas[i] - medianAbsoluteDelta);
        }
        final double medianAbsoluteDeviation = ordinaryAbsoluteCount == 0 ? 0D : median(absoluteDeviations);
        final double discontinuityThreshold = Math.max(minimumDiscontinuity,
                                                       medianAbsoluteDelta +
                                                       DISCONTINUITY_MAD_MULTIPLIER * medianAbsoluteDeviation);

        final double[] ordinaryDeltas = new double[deltas.length];
        int ordinaryDeltaCount = 0;
        for (int i = 0; i < deltas.length; i++) {
            if (!trustedBreakBefore[i + 1] && Math.abs(deltas[i]) <= discontinuityThreshold) {
                ordinaryDeltas[ordinaryDeltaCount++] = deltas[i];
            }
        }
        final double expectedDelta = ordinaryDeltaCount == 0
                                     ? 0D
                                     : median(Arrays.copyOf(ordinaryDeltas, ordinaryDeltaCount));

        final double[] corrected = new double[values.length];
        corrected[0] = values[0];
        double accumulatedCorrection = 0D;
        for (int i = 1; i < values.length; i++) {
            final double delta = values[i] - values[i - 1];
            if (trustedBreakBefore[i] || Math.abs(delta) > discontinuityThreshold) {
                final double localExpectedDelta = localExpectedDelta(deltas,
                                                                     trustedBreakBefore,
                                                                     i - 1,
                                                                     discontinuityThreshold,
                                                                     expectedDelta);
                accumulatedCorrection += delta - localExpectedDelta;
            }
            corrected[i] = values[i] - accumulatedCorrection;
        }
        return corrected;
    }

    private static double localExpectedDelta(final double[] deltas,
                                             final boolean[] trustedBreakBefore,
                                             final int discontinuityIndex,
                                             final double discontinuityThreshold,
                                             final double fallback)
    {
        final double[] nearby = new double[8];
        int count = 0;
        for (int distance = 1; distance <= 4; distance++) {
            final int before = discontinuityIndex - distance;
            if (before >= 0 &&
                !trustedBreakBefore[before + 1] &&
                Math.abs(deltas[before]) <= discontinuityThreshold) nearby[count++] = deltas[before];
            final int after = discontinuityIndex + distance;
            if (after < deltas.length &&
                !trustedBreakBefore[after + 1] &&
                Math.abs(deltas[after]) <= discontinuityThreshold) nearby[count++] = deltas[after];
        }
        return count == 0 ? fallback : median(Arrays.copyOf(nearby, count));
    }

    private static double median(final double[] values)
    {
        final double[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        final int middle = sorted.length / 2;
        return (sorted.length & 1) == 1 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) * 0.5D;
    }

    /**
     * Fits {@code a + bx + cx²} for equally spaced x values in [-1, 1] and returns the residuals.
     * Symmetry keeps the normal equations small enough to solve without matrix allocation.
     */
    private static double[] detrendQuadratic(final double[] values)
    {
        final int length = values.length;
        double sumY = 0D;
        double sumXY = 0D;
        double sumX2Y = 0D;
        double sumX2 = 0D;
        double sumX4 = 0D;

        for (int i = 0; i < length; i++) {
            final double x = normalizedIndex(i, length);
            final double xSquared = x * x;
            sumY += values[i];
            sumXY += x * values[i];
            sumX2Y += xSquared * values[i];
            sumX2 += xSquared;
            sumX4 += xSquared * xSquared;
        }

        final double determinant = length * sumX4 - sumX2 * sumX2;
        final double a = (sumY * sumX4 - sumX2 * sumX2Y) / determinant;
        final double b = sumXY / sumX2;
        final double c = (length * sumX2Y - sumX2 * sumY) / determinant;

        final double[] residuals = new double[length];
        for (int i = 0; i < length; i++) {
            final double x = normalizedIndex(i, length);
            residuals[i] = values[i] - (a + b * x + c * x * x);
        }
        return residuals;
    }

    private static double[] unwrapYaw(final double[] yaw)
    {
        final double[] result = Arrays.copyOf(yaw, yaw.length);
        for (int i = 1; i < result.length; i++) {
            result[i] = result[i - 1] + signedYawDelta(yaw[i], yaw[i - 1]);
        }
        return result;
    }

    private static double normalizedIndex(final int index, final int length)
    {
        return 2D * index / (length - 1D) - 1D;
    }

    private static void center(final double[] values)
    {
        double mean = 0D;
        for (double value : values) mean += value;
        mean /= values.length;
        for (int i = 0; i < values.length; i++) values[i] -= mean;
    }

    private static double[] normalizeResiduals(final double[] residuals, final double standardDeviation)
    {
        final double[] normalized = new double[residuals.length];
        if (!Double.isFinite(standardDeviation) || standardDeviation == 0D) return normalized;
        for (int i = 0; i < residuals.length; i++) normalized[i] = residuals[i] / standardDeviation;
        return normalized;
    }

    private static double[] normalizeFingerprint(final double[] residuals, final AxisResult result)
    {
        return result.pattern() == Pattern.PRECISE
               ? new double[residuals.length]
               : normalizeResiduals(residuals, result.standardDeviation());
    }

    private static double meanSquare(final double[] values, final int fromInclusive, final int toExclusive)
    {
        double sum = 0D;
        for (int i = fromInclusive; i < toExclusive; i++) sum += values[i] * values[i];
        return sum / (toExclusive - fromInclusive);
    }

    private static double maximumAbsolute(final double[] values)
    {
        double maximum = 0D;
        for (double value : values) maximum = Math.max(maximum, Math.abs(value));
        return maximum;
    }

    private static double range(final double[] values)
    {
        double minimum = values[0];
        double maximum = values[0];
        for (int i = 1; i < values.length; i++) {
            minimum = Math.min(minimum, values[i]);
            maximum = Math.max(maximum, values[i]);
        }
        return maximum - minimum;
    }

    private static double signChangeRatio(final double[] values)
    {
        int signChanges = 0;
        int comparablePairs = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i - 1] == 0D || values[i] == 0D) continue;
            comparablePairs++;
            if (values[i - 1] * values[i] < 0D) signChanges++;
        }
        return comparablePairs == 0 ? 0D : signChanges / (double) comparablePairs;
    }

    private static double permutationEntropy(final double[] values)
    {
        final int[] counts = new int[6];
        for (int i = 0; i < values.length - 2; i++) counts[ordinalPattern(values[i], values[i + 1], values[i + 2])]++;

        final int total = values.length - 2;
        double entropy = 0D;
        for (int count : counts) {
            if (count == 0) continue;
            final double probability = count / (double) total;
            entropy -= probability * Math.log(probability);
        }
        return entropy / Math.log(6D);
    }

    private static int ordinalPattern(final double first, final double second, final double third)
    {
        if (first <= second) {
            if (second <= third) return 0;
            return first <= third ? 1 : 2;
        }
        if (first <= third) return 3;
        return second <= third ? 4 : 5;
    }

    private static double runsZScore(final double[] values)
    {
        int positive = 0;
        int negative = 0;
        int runs = 0;
        int previousSign = 0;
        for (double value : values) {
            final int sign = value > 0D ? 1 : value < 0D ? -1 : 0;
            if (sign == 0) continue;
            if (sign > 0) positive++;
            else negative++;
            if (sign != previousSign) runs++;
            previousSign = sign;
        }

        final int total = positive + negative;
        if (positive == 0 || negative == 0 || total < 4) return Double.POSITIVE_INFINITY;
        final double expected = 1D + 2D * positive * negative / total;
        final double variance = 2D * positive * negative * (2D * positive * negative - total) /
                                ((double) total * total * (total - 1D));
        return variance <= 0D ? 0D : (runs - expected) / Math.sqrt(variance);
    }

    private static double distinctLevelRatio(final double[] normalizedResiduals)
    {
        final int[] levels = new int[normalizedResiduals.length];
        int distinct = 0;
        for (double residual : normalizedResiduals) {
            final int level = (int) Math.rint(residual * 8D);
            boolean known = false;
            for (int i = 0; i < distinct; i++) {
                if (levels[i] == level) {
                    known = true;
                    break;
                }
            }
            if (!known) levels[distinct++] = level;
        }
        return distinct / (double) normalizedResiduals.length;
    }

    private static Periodicity periodicity(final double[] normalizedResiduals)
    {
        final int maximumLag = normalizedResiduals.length / 2;
        double bestCorrelation = 0D;
        double bestRepeatError = Double.POSITIVE_INFINITY;
        int bestLag = 0;

        for (int lag = 2; lag <= maximumLag; lag++) {
            final double signedCorrelation = correlation(normalizedResiduals, normalizedResiduals, lag);
            final double absoluteCorrelation = Math.abs(signedCorrelation);
            final double repeatError = repeatError(normalizedResiduals, lag, signedCorrelation < 0D ? -1D : 1D);
            if (absoluteCorrelation > bestCorrelation ||
                (absoluteCorrelation == bestCorrelation && repeatError < bestRepeatError)) {
                bestCorrelation = absoluteCorrelation;
                bestRepeatError = repeatError;
                bestLag = lag;
            }
        }
        return new Periodicity(bestCorrelation, bestLag, bestRepeatError);
    }

    private static double repeatError(final double[] values, final int lag, final double sign)
    {
        double squaredError = 0D;
        final int length = values.length - lag;
        for (int i = 0; i < length; i++) {
            final double difference = values[i + lag] - sign * values[i];
            squaredError += difference * difference;
        }
        return Math.sqrt(squaredError / length);
    }

    /**
     * Pearson correlation. For autocorrelation, {@code lag} offsets the second sequence.
     */
    private static double correlation(final double[] first, final double[] second, final int lag)
    {
        final int length = Math.min(first.length, second.length) - lag;
        if (length <= 1) return 1D;

        double firstMean = 0D;
        double secondMean = 0D;
        for (int i = 0; i < length; i++) {
            firstMean += first[i];
            secondMean += second[i + lag];
        }
        firstMean /= length;
        secondMean /= length;

        double covariance = 0D;
        double firstVariance = 0D;
        double secondVariance = 0D;
        for (int i = 0; i < length; i++) {
            final double centeredFirst = first[i] - firstMean;
            final double centeredSecond = second[i + lag] - secondMean;
            covariance += centeredFirst * centeredSecond;
            firstVariance += centeredFirst * centeredFirst;
            secondVariance += centeredSecond * centeredSecond;
        }

        final double denominator = Math.sqrt(firstVariance * secondVariance);
        return denominator == 0D ? 1D : covariance / denominator;
    }

    private static void validateFinite(final double[] values)
    {
        for (double value : values) {
            if (!Double.isFinite(value)) throw new IllegalArgumentException("rotation samples must be finite");
        }
    }

    /**
     * Classification assigned to the stochastic residual shape of one rotation axis.
     */
    public enum Pattern
    {
        NATURAL,
        PRECISE,
        UNIFORM,
        GAUSSIAN,
        DISTRIBUTION_FREE;

        /**
         * @return true for any supported randomized residual distribution
         */
        public boolean isRandomized()
        {
            return this == UNIFORM || this == GAUSSIAN || this == DISTRIBUTION_FREE;
        }
    }

    /**
     * Deterministic patterns commonly used instead of ordinary random noise.
     */
    public enum SyntheticPattern
    {
        NONE,
        PERIODIC,
        ALTERNATING,
        LOW_ENTROPY
    }

    /**
     * Combined yaw and pitch result.
     */
    public record Result(AxisResult yaw,
                         AxisResult pitch,
                         double crossCorrelation,
                         double[] yawFingerprint,
                         double[] pitchFingerprint)
    {
        public Result
        {
            yawFingerprint = Arrays.copyOf(yawFingerprint, yawFingerprint.length);
            pitchFingerprint = Arrays.copyOf(pitchFingerprint, pitchFingerprint.length);
        }

        @Override
        public double[] yawFingerprint()
        {
            return Arrays.copyOf(yawFingerprint, yawFingerprint.length);
        }

        @Override
        public double[] pitchFingerprint()
        {
            return Arrays.copyOf(pitchFingerprint, pitchFingerprint.length);
        }

        public int randomizedAxisCount()
        {
            return (yaw.pattern().isRandomized() ? 1 : 0) + (pitch.pattern().isRandomized() ? 1 : 0);
        }

        public int preciseAxisCount()
        {
            return (yaw.pattern() == Pattern.PRECISE ? 1 : 0) + (pitch.pattern() == Pattern.PRECISE ? 1 : 0);
        }

        public int syntheticAxisCount()
        {
            return (yaw.syntheticPattern() != SyntheticPattern.NONE ? 1 : 0) +
                   (pitch.syntheticPattern() != SyntheticPattern.NONE ? 1 : 0);
        }
    }

    /**
     * Statistical metrics and classifications for one axis.
     */
    public record AxisResult(Pattern pattern,
                             SyntheticPattern syntheticPattern,
                             double standardDeviation,
                             double maxAbsoluteResidual,
                             double rotationRange,
                             double randomnessScore,
                             double lagOneAutocorrelation,
                             double averageAbsAutocorrelation,
                             double signChangeRatio,
                             double ksDStatistic,
                             double ksPValue,
                             double skewness,
                             double kurtosis,
                             double varianceRatio,
                             double permutationEntropy,
                             double runsZScore,
                             double distinctLevelRatio,
                             double maxPeriodicCorrelation,
                             int periodicLag,
                             double repeatError,
                             int randomWindowCount,
                             int syntheticWindowCount)
    {
        private static AxisResult precise(final double standardDeviation,
                                          final double maxAbsoluteResidual,
                                          final double rotationRange)
        {
            return new AxisResult(Pattern.PRECISE,
                                  SyntheticPattern.NONE,
                                  standardDeviation,
                                  maxAbsoluteResidual,
                                  rotationRange,
                                  0D,
                                  1D,
                                  1D,
                                  0D,
                                  1D,
                                  0D,
                                  0D,
                                  0D,
                                  Double.POSITIVE_INFINITY,
                                  0D,
                                  Double.POSITIVE_INFINITY,
                                  0D,
                                  0D,
                                  0,
                                  Double.POSITIVE_INFINITY,
                                  0,
                                  0);
        }

        private static AxisResult natural(final double standardDeviation,
                                          final double maxAbsoluteResidual,
                                          final double rotationRange)
        {
            return new AxisResult(Pattern.NATURAL,
                                  SyntheticPattern.NONE,
                                  standardDeviation,
                                  maxAbsoluteResidual,
                                  rotationRange,
                                  0D,
                                  1D,
                                  1D,
                                  0D,
                                  1D,
                                  0D,
                                  0D,
                                  0D,
                                  Double.POSITIVE_INFINITY,
                                  0D,
                                  Double.POSITIVE_INFINITY,
                                  0D,
                                  0D,
                                  0,
                                  Double.POSITIVE_INFINITY,
                                  0,
                                  0);
        }

        private AxisResult withClassifications(final Pattern newPattern,
                                               final SyntheticPattern newSyntheticPattern,
                                               final int newRandomWindowCount,
                                               final int newSyntheticWindowCount,
                                               final double fullStandardDeviation,
                                               final double fullMaxAbsoluteResidual,
                                               final double fullRotationRange)
        {
            return new AxisResult(newPattern,
                                  newSyntheticPattern,
                                  fullStandardDeviation,
                                  fullMaxAbsoluteResidual,
                                  fullRotationRange,
                                  randomnessScore,
                                  lagOneAutocorrelation,
                                  averageAbsAutocorrelation,
                                  signChangeRatio,
                                  ksDStatistic,
                                  ksPValue,
                                  skewness,
                                  kurtosis,
                                  varianceRatio,
                                  permutationEntropy,
                                  runsZScore,
                                  distinctLevelRatio,
                                  maxPeriodicCorrelation,
                                  periodicLag,
                                  repeatError,
                                  newRandomWindowCount,
                                  newSyntheticWindowCount);
        }
    }

    private record AxisAnalysis(AxisResult result, double[] residuals)
    {
    }

    private record Periodicity(double correlation, int lag, double repeatError)
    {
    }
}
