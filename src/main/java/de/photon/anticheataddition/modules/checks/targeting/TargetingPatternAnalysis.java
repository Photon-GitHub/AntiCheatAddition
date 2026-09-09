package de.photon.anticheataddition.modules.checks.targeting;

import static de.photon.anticheataddition.modules.checks.targeting.TargetingAnalysis.SyntheticPattern;
import static de.photon.anticheataddition.util.mathematics.DataUtil.correlation;

/** Identifies repeated, alternating and low-entropy residual sequences using the established thresholds. */
final class TargetingPatternAnalysis
{
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

    private TargetingPatternAnalysis() {}

    static Result analyze(final double[] normalizedResiduals,
                          final double lagOne,
                          final double lagTwo,
                          final double signChangeRatio,
                          final double permutationEntropy)
    {
        final double distinctLevelRatio = distinctLevelRatio(normalizedResiduals);
        final Periodicity periodicity = periodicity(normalizedResiduals);
        return new Result(classifySyntheticPattern(lagOne, lagTwo, signChangeRatio, permutationEntropy,
                                                   distinctLevelRatio, periodicity), distinctLevelRatio, periodicity);
    }

    static boolean isStrongFullPeriodic(final TargetingAnalysis.AxisResult full)
    {
        return full.syntheticPattern() == SyntheticPattern.PERIODIC &&
               full.maxPeriodicCorrelation() >= STRONG_FULL_PERIODIC_MIN_CORRELATION &&
               full.repeatError() <= STRONG_FULL_PERIODIC_MAX_REPEAT_ERROR;
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

    record Result(SyntheticPattern pattern, double distinctLevelRatio, Periodicity periodicity) {}

    record Periodicity(double correlation, int lag, double repeatError) {}
}
