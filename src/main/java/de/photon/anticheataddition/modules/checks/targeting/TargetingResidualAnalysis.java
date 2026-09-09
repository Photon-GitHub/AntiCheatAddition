package de.photon.anticheataddition.modules.checks.targeting;

import static de.photon.anticheataddition.modules.checks.targeting.TargetingAnalysis.*;
import static de.photon.anticheataddition.modules.checks.targeting.TargetingDiscontinuityCorrection.removeDiscontinuities;
import static de.photon.anticheataddition.util.mathematics.DataUtil.*;
import static de.photon.anticheataddition.util.mathematics.TimeSeriesUtil.*;

/** Prepares one rotation axis and shares its residual measurements with the isolated classifiers. */
final class TargetingResidualAnalysis
{
    private static final double PRECISE_MAX_STANDARD_DEVIATION = 0.0005D;
    private static final double PRECISE_MAX_ABSOLUTE_RESIDUAL = 0.0015D;
    private static final double PITCH_CLAMP_EPSILON = 0.0001D;
    private static final double PITCH_CLAMP_MINIMUM_RATIO = 0.75D;
    private static final double PITCH_MINIMUM_DISCONTINUITY = 15D;

    private TargetingResidualAnalysis() {}

    /**
     * Pitch is hard-clamped by the vanilla client at straight up and straight down. A long clamped section can be
     * mathematically precise without representing automated targeting, so only the precision classification is
     * suppressed. Other residual and deterministic classifications remain available.
     */
    static AxisAnalysis analyzePitchAxis(final double[] rotations, final boolean[] trustedBreakBefore)
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

    static AxisAnalysis analyzeAxis(final double[] rotations,
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
        final double lagOne = correlation(residuals, residuals, 1);
        final double lagTwo = correlation(residuals, residuals, 2);
        final double lagThree = correlation(residuals, residuals, 3);
        final double averageAbsAutocorrelation = (Math.abs(lagOne) + Math.abs(lagTwo) + Math.abs(lagThree)) / 3D;
        final double signChangeRatio = signChangeRatio(residuals);
        final double permutationEntropy = permutationEntropy(normalizedResiduals);
        final double runsZScore = runsZScore(normalizedResiduals);

        final int midpoint = residuals.length / 2;
        final double firstVariance = meanSquare(residuals, 0, midpoint);
        final double secondVariance = meanSquare(residuals, midpoint, residuals.length);
        final double varianceRatio = secondVariance == 0D ? Double.POSITIVE_INFINITY : firstVariance / secondVariance;

        final var noise = TargetingNoiseAnalysis.analyze(residuals, normalizedResiduals, lagOne,
                                                         averageAbsAutocorrelation, signChangeRatio, varianceRatio,
                                                         permutationEntropy, runsZScore);
        final var synthetic = TargetingPatternAnalysis.analyze(normalizedResiduals, lagOne, lagTwo,
                                                               signChangeRatio, permutationEntropy);
        final Pattern pattern = noise.pattern();
        final SyntheticPattern syntheticPattern = synthetic.pattern();
        final var periodicity = synthetic.periodicity();

        return new AxisAnalysis(new AxisResult(pattern,
                                               syntheticPattern,
                                               standardDeviation,
                                               maxAbsoluteResidual,
                                               rotationRange,
                                               noise.score(),
                                               lagOne,
                                               averageAbsAutocorrelation,
                                               signChangeRatio,
                                               noise.ksDStatistic(),
                                               noise.ksPValue(),
                                               noise.skewness(),
                                               noise.kurtosis(),
                                               varianceRatio,
                                               permutationEntropy,
                                               runsZScore,
                                               synthetic.distinctLevelRatio(),
                                               periodicity.correlation(),
                                               periodicity.lag(),
                                               periodicity.repeatError(),
                                               pattern.isRandomized() ? 1 : 0,
                                               syntheticPattern == SyntheticPattern.NONE ? 0 : 1),
                                residuals);
    }

    static double[] normalizeResiduals(final double[] residuals, final double standardDeviation)
    {
        final double[] normalized = new double[residuals.length];
        if (!Double.isFinite(standardDeviation) || standardDeviation == 0D) return normalized;
        for (int i = 0; i < residuals.length; i++) normalized[i] = residuals[i] / standardDeviation;
        return normalized;
    }

    record AxisAnalysis(AxisResult result, double[] residuals) {}
}
