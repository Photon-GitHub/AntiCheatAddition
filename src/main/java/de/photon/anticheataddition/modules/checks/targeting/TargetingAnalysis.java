package de.photon.anticheataddition.modules.checks.targeting;

import de.photon.anticheataddition.util.mathematics.MathUtil;

import java.util.Arrays;

import static de.photon.anticheataddition.modules.checks.targeting.TargetingResidualAnalysis.*;
import static de.photon.anticheataddition.util.mathematics.DataUtil.correlation;

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

    private static final double YAW_MINIMUM_DISCONTINUITY = 25D;

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
        final boolean strongFullPeriodic = TargetingPatternAnalysis.isStrongFullPeriodic(full);
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

    private static double[] normalizeFingerprint(final double[] residuals, final AxisResult result)
    {
        return result.pattern() == Pattern.PRECISE
               ? new double[residuals.length]
               : normalizeResiduals(residuals, result.standardDeviation());
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
        static AxisResult precise(final double standardDeviation,
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

        static AxisResult natural(final double standardDeviation,
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

}
