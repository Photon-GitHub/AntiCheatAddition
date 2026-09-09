package de.photon.anticheataddition.modules.checks.targeting;

import de.photon.anticheataddition.util.mathematics.KolmogorovSmirnov;

import static de.photon.anticheataddition.modules.checks.targeting.TargetingAnalysis.Pattern;

/** Classifies stochastic residuals; distribution fit alone never supplies targeting evidence. */
final class TargetingNoiseAnalysis
{
    private static final double RANDOM_MINIMUM_SCORE = 0.78D;
    private static final double DISTRIBUTION_FREE_MINIMUM_SCORE = 0.68D;
    private static final double RANDOM_LAG_ONE_SCALE = 0.6D;
    private static final double RANDOM_AVERAGE_AUTOCORRELATION_SCALE = 0.45D;
    private static final double RANDOM_VARIANCE_LOG_SCALE = 2.5D;
    private static final double RANDOM_RUNS_Z_SCALE = 3.5D;
    private static final double UNIFORM_MIN_P_VALUE = 0.1D;
    private static final double UNIFORM_MAX_D_STATISTIC = 0.22D;
    private static final double GAUSSIAN_MAX_ABS_SKEWNESS = 0.9D;
    private static final double GAUSSIAN_MIN_KURTOSIS = 1.7D;
    private static final double GAUSSIAN_MAX_KURTOSIS = 5D;

    private TargetingNoiseAnalysis() {}

    static Result analyze(final double[] residuals,
                          final double[] normalizedResiduals,
                          final double lagOne,
                          final double averageAbsAutocorrelation,
                          final double signChangeRatio,
                          final double varianceRatio,
                          final double permutationEntropy,
                          final double runsZScore)
    {
        double thirdMoment = 0D;
        double fourthMoment = 0D;
        for (double standardized : normalizedResiduals) {
            final double squared = standardized * standardized;
            thirdMoment += squared * standardized;
            fourthMoment += squared * squared;
        }

        final double skewness = thirdMoment / residuals.length;
        final double kurtosis = fourthMoment / residuals.length;
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

        return new Result(pattern, randomnessScore, ksResult.dStatistic(), ksResult.pValue(), skewness, kurtosis);
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

    record Result(Pattern pattern, double score, double ksDStatistic, double ksPValue,
                  double skewness, double kurtosis) {}
}
