package de.photon.anticheataddition.modules.checks.targeting;

import de.photon.anticheataddition.util.mathematics.KolmogorovSmirnov;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import java.util.Arrays;

import static de.photon.anticheataddition.modules.checks.targeting.TargetingDiscontinuityCorrection.removeDiscontinuities;
import static de.photon.anticheataddition.util.mathematics.DataUtil.*;
import static de.photon.anticheataddition.util.mathematics.TimeSeriesUtil.*;

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
     * Canonicalizes any finite yaw without subtracting the potentially very large raw input values.
     *
     * @return a yaw in [-180, 180), or NaN for a non-finite input
     */
    public static double normalizeYaw(final double yaw)
    {
        return MathUtil.normalizeYaw(yaw);
    }

    /** Calculates the shortest signed yaw delta in degrees. */
    public static double signedYawDelta(final double currentYaw, final double previousYaw)
    {
        return MathUtil.signedYawDelta(currentYaw, previousYaw);
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

    /** Starts from a canonical yaw so large raw angles cannot swallow subsequent deltas. */
    private static double[] unwrapYaw(final double[] yaw)
    {
        final double[] result = new double[yaw.length];
        result[0] = normalizeYaw(yaw[0]);
        for (int i = 1; i < result.length; i++) {
            result[i] = result[i - 1] + signedYawDelta(yaw[i], yaw[i - 1]);
        }
        return result;
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
