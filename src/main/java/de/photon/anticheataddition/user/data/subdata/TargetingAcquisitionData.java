package de.photon.anticheataddition.user.data.subdata;

import de.photon.anticheataddition.modules.checks.targeting.TargetingAcquisitionAnalysis;

import java.util.Arrays;

/**
 * Per-user history of independent target-acquisition profiles.
 *
 * <p>A single smooth or highly accurate acquisition is ordinary player behavior. This class only reports suspicious
 * behavior when many valid acquisitions at comparable target distances repeatedly exhibit nearly the same target-bound
 * slowdown curve, activation distance, and suppression strength.</p>
 */
public final class TargetingAcquisitionData {
    private static final int HISTORY_CAPACITY = 32;
    private static final int MINIMUM_COMPARABLE_PROFILES = 12;
    private static final int MINIMUM_CANDIDATE_PROFILES = 9;
    private static final int MINIMUM_ACTIVATION_PROFILES = 9;
    private static final double MAXIMUM_DISTANCE_RATIO = 1.35D;
    private static final double MAXIMUM_MEAN_SLOWDOWN_RATIO = 0.54D;
    private static final double MAXIMUM_SLOWDOWN_STANDARD_DEVIATION = 0.085D;
    private static final double MAXIMUM_ACTIVATION_COEFFICIENT_OF_VARIATION = 0.15D;
    private static final double MINIMUM_PROFILE_CORRELATION = 0.93D;
    private static final double MAXIMUM_PROFILE_ROOT_MEAN_SQUARE_ERROR = 0.13D;
    private static final double MINIMUM_MEAN_APPROACH_EFFICIENCY = 0.74D;
    private static final double MINIMUM_MEAN_TOWARD_RATIO = 0.72D;

    private final TargetingAcquisitionAnalysis.Profile[] history =
            new TargetingAcquisitionAnalysis.Profile[HISTORY_CAPACITY];
    private int writeIndex;
    private int size;

    /**
     * Stores one valid acquisition and evaluates recent comparable acquisitions.
     *
     * @return a conservative aggregate assessment; fewer than twelve comparable acquisitions cannot be suspicious
     */
    public synchronized Assessment add(final TargetingAcquisitionAnalysis.Profile profile)
    {
        if (profile == null) throw new NullPointerException("profile must not be null");
        history[writeIndex] = profile;
        writeIndex = (writeIndex + 1) % HISTORY_CAPACITY;
        if (size < HISTORY_CAPACITY) size++;
        return assess(profile.targetDistance());
    }

    /**
     * Clears profile history, intended only for full user destruction or explicit administrative reset.
     */
    public synchronized void clear()
    {
        Arrays.fill(history, null);
        writeIndex = 0;
        size = 0;
    }

    /**
     * @return the number of retained valid acquisition profiles
     */
    public synchronized int size()
    {
        return size;
    }

    private Assessment assess(final double referenceDistance)
    {
        final TargetingAcquisitionAnalysis.Profile[] comparable = new TargetingAcquisitionAnalysis.Profile[size];
        int comparableCount = 0;
        int candidateCount = 0;
        int activationCount = 0;

        for (int logicalIndex = 0; logicalIndex < size; logicalIndex++) {
            final TargetingAcquisitionAnalysis.Profile profile = history[physicalIndex(logicalIndex)];
            if (profile == null || !comparableDistance(profile.targetDistance(), referenceDistance)) continue;
            comparable[comparableCount++] = profile;
            if (profile.controlledSlowdownCandidate()) candidateCount++;
            if (profile.controlledSlowdownCandidate() &&
                Double.isFinite(profile.activationError()) &&
                profile.activationDrop() >= 0.20D) activationCount++;
        }

        if (comparableCount < MINIMUM_COMPARABLE_PROFILES) {
            return Assessment.insufficient(comparableCount, candidateCount);
        }

        final double[] slowdown = new double[comparableCount];
        final double[] approachEfficiency = new double[comparableCount];
        final double[] towardRatio = new double[comparableCount];
        final double[] activation = new double[activationCount];
        final double[][] profiles = new double[comparableCount][];
        int activationIndex = 0;
        for (int i = 0; i < comparableCount; i++) {
            final TargetingAcquisitionAnalysis.Profile profile = comparable[i];
            slowdown[i] = profile.slowdownRatio();
            approachEfficiency[i] = profile.approachEfficiency();
            towardRatio[i] = profile.towardRatio();
            profiles[i] = profile.normalizedSpeedProfile();
            if (profile.controlledSlowdownCandidate() &&
                Double.isFinite(profile.activationError()) &&
                profile.activationDrop() >= 0.20D) {
                activation[activationIndex++] = profile.activationError();
            }
        }

        final double[] centroid = centroid(profiles);
        double correlationSum = 0D;
        double squaredErrorSum = 0D;
        for (double[] profile : profiles) {
            correlationSum += correlation(profile, centroid);
            for (int bin = 0; bin < profile.length; bin++) {
                final double difference = profile[bin] - centroid[bin];
                squaredErrorSum += difference * difference;
            }
        }

        final double candidateRatio = candidateCount / (double) comparableCount;
        final double meanSlowdown = mean(slowdown);
        final double slowdownStandardDeviation = standardDeviation(slowdown);
        final double activationVariation = coefficientOfVariation(activation);
        final double meanProfileCorrelation = correlationSum / comparableCount;
        final double profileRootMeanSquareError = Math.sqrt(squaredErrorSum /
                                                            (comparableCount * TargetingAcquisitionAnalysis.PROFILE_BIN_COUNT));
        final double meanApproachEfficiency = mean(approachEfficiency);
        final double meanTowardRatio = mean(towardRatio);

        int independentSignals = 0;
        if (candidateCount >= MINIMUM_CANDIDATE_PROFILES && candidateRatio >= 0.72D) independentSignals++;
        if (meanSlowdown <= MAXIMUM_MEAN_SLOWDOWN_RATIO) independentSignals++;
        if (slowdownStandardDeviation <= MAXIMUM_SLOWDOWN_STANDARD_DEVIATION) independentSignals++;
        if (activationCount >= MINIMUM_ACTIVATION_PROFILES &&
            Double.isFinite(activationVariation) &&
            activationVariation <= MAXIMUM_ACTIVATION_COEFFICIENT_OF_VARIATION) independentSignals++;
        if (meanProfileCorrelation >= MINIMUM_PROFILE_CORRELATION) independentSignals++;
        if (profileRootMeanSquareError <= MAXIMUM_PROFILE_ROOT_MEAN_SQUARE_ERROR) independentSignals++;
        if (meanApproachEfficiency >= MINIMUM_MEAN_APPROACH_EFFICIENCY) independentSignals++;
        if (meanTowardRatio >= MINIMUM_MEAN_TOWARD_RATIO) independentSignals++;

        final boolean stableActivation = activationCount >= MINIMUM_ACTIVATION_PROFILES &&
                                         Double.isFinite(activationVariation) &&
                                         activationVariation <= MAXIMUM_ACTIVATION_COEFFICIENT_OF_VARIATION;
        final boolean suspicious = independentSignals >= 6 &&
                                   candidateCount >= MINIMUM_CANDIDATE_PROFILES &&
                                   stableActivation &&
                                   meanSlowdown <= MAXIMUM_MEAN_SLOWDOWN_RATIO &&
                                   meanProfileCorrelation >= MINIMUM_PROFILE_CORRELATION;
        final boolean severe = suspicious &&
                               independentSignals >= 7 &&
                               candidateRatio >= 0.85D &&
                               meanSlowdown <= 0.45D &&
                               profileRootMeanSquareError <= 0.09D;

        return new Assessment(true,
                              suspicious,
                              severe,
                              comparableCount,
                              candidateCount,
                              independentSignals,
                              candidateRatio,
                              meanSlowdown,
                              slowdownStandardDeviation,
                              activationVariation,
                              meanProfileCorrelation,
                              profileRootMeanSquareError,
                              meanApproachEfficiency,
                              meanTowardRatio);
    }

    private int physicalIndex(final int logicalIndex)
    {
        final int oldest = (writeIndex - size + HISTORY_CAPACITY) % HISTORY_CAPACITY;
        return (oldest + logicalIndex) % HISTORY_CAPACITY;
    }

    private static boolean comparableDistance(final double first, final double second)
    {
        if (!Double.isFinite(first) || !Double.isFinite(second) || first <= 0D || second <= 0D) return false;
        final double ratio = Math.max(first, second) / Math.min(first, second);
        return ratio <= MAXIMUM_DISTANCE_RATIO;
    }

    private static double[] centroid(final double[][] profiles)
    {
        final double[] result = new double[TargetingAcquisitionAnalysis.PROFILE_BIN_COUNT];
        for (double[] profile : profiles) {
            for (int bin = 0; bin < result.length; bin++) result[bin] += profile[bin];
        }
        for (int bin = 0; bin < result.length; bin++) result[bin] /= profiles.length;
        return result;
    }

    private static double mean(final double[] values)
    {
        if (values.length == 0) return Double.NaN;
        double sum = 0D;
        for (double value : values) sum += value;
        return sum / values.length;
    }

    private static double standardDeviation(final double[] values)
    {
        if (values.length < 2) return Double.POSITIVE_INFINITY;
        final double mean = mean(values);
        double squareSum = 0D;
        for (double value : values) {
            final double difference = value - mean;
            squareSum += difference * difference;
        }
        return Math.sqrt(squareSum / (values.length - 1D));
    }

    private static double coefficientOfVariation(final double[] values)
    {
        if (values.length < 2) return Double.NaN;
        final double mean = mean(values);
        if (!Double.isFinite(mean) || Math.abs(mean) <= 1E-9D) return Double.NaN;
        return standardDeviation(values) / Math.abs(mean);
    }

    private static double correlation(final double[] first, final double[] second)
    {
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

    /**
     * Aggregate evidence from recent comparable successful-hit acquisitions.
     */
    public record Assessment(boolean enoughData,
                             boolean suspicious,
                             boolean severe,
                             int comparableProfiles,
                             int candidateProfiles,
                             int independentSignals,
                             double candidateRatio,
                             double meanSlowdownRatio,
                             double slowdownStandardDeviation,
                             double activationCoefficientOfVariation,
                             double meanProfileCorrelation,
                             double profileRootMeanSquareError,
                             double meanApproachEfficiency,
                             double meanTowardRatio) {
        public static Assessment insufficient(final int comparableProfiles, final int candidateProfiles)
        {
            return new Assessment(false,
                                  false,
                                  false,
                                  comparableProfiles,
                                  candidateProfiles,
                                  0,
                                  0D,
                                  Double.NaN,
                                  Double.NaN,
                                  Double.NaN,
                                  Double.NaN,
                                  Double.NaN,
                                  Double.NaN,
                                  Double.NaN);
        }
    }
}
