package de.photon.anticheataddition.user.data.subdata;

import de.photon.anticheataddition.modules.checks.targeting.TargetingAnalysis;
import de.photon.anticheataddition.modules.checks.targeting.TargetingContext;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;

/**
 * Per-user history used by the Targeting Replay submodule.
 *
 * <p>The stored fingerprints are normalized residual traces, so replay remains detectable when a client changes the
 * absolute aim angle, scales the trace amplitude, mirrors an axis, or replays the trace backwards. Overlapping packet
 * ranges are never compared because consecutive Targeting snapshots intentionally share many samples.</p>
 */
public final class TargetingReplayData {
    private static final int FINGERPRINT_LENGTH = 24;
    private static final int MAX_HISTORY_SIZE = 8;
    private static final int MAX_SHIFT = 2;
    private static final double MINIMUM_REPLAY_CORRELATION = 0.985D;
    private static final double MAXIMUM_REPLAY_ERROR = 0.25D;

    private final Map<TargetingContext, Deque<Entry>> history = new EnumMap<>(TargetingContext.class);

    public TargetingReplayData()
    {
        for (TargetingContext context : TargetingContext.values()) history.put(context, new ArrayDeque<>());
    }

    /**
     * Compares a completed analysis window with older non-overlapping windows and then remembers the new one.
     *
     * @return the best replay similarity found for yaw and pitch
     */
    public synchronized ReplayResult compareAndRemember(final TargetingContext context,
                                                        final long firstSequence,
                                                        final long lastSequence,
                                                        final TargetingAnalysis.Result result)
    {
        if (context == null || result == null) throw new NullPointerException("context and result must not be null");

        final double[] yawFingerprint = resample(result.yawFingerprint());
        final double[] pitchFingerprint = resample(result.pitchFingerprint());
        final boolean yawEligible = hasVariation(yawFingerprint);
        final boolean pitchEligible = hasVariation(pitchFingerprint);
        final Deque<Entry> entries = history.get(context);

        Similarity bestYaw = Similarity.none();
        Similarity bestPitch = Similarity.none();
        for (Entry entry : entries) {
            if (entry.lastSequence() >= firstSequence) continue;
            if (yawEligible && entry.yawEligible()) {
                final Similarity similarity = bestSimilarity(yawFingerprint, entry.yawFingerprint());
                if (similarity.correlation() > bestYaw.correlation()) bestYaw = similarity;
            }
            if (pitchEligible && entry.pitchEligible()) {
                final Similarity similarity = bestSimilarity(pitchFingerprint, entry.pitchFingerprint());
                if (similarity.correlation() > bestPitch.correlation()) bestPitch = similarity;
            }
        }

        entries.addLast(new Entry(lastSequence,
                                  yawEligible,
                                  pitchEligible,
                                  yawFingerprint,
                                  pitchFingerprint));
        while (entries.size() > MAX_HISTORY_SIZE) entries.removeFirst();

        final boolean replayedYaw = bestYaw.correlation() >= MINIMUM_REPLAY_CORRELATION &&
                                    bestYaw.error() <= MAXIMUM_REPLAY_ERROR;
        final boolean replayedPitch = bestPitch.correlation() >= MINIMUM_REPLAY_CORRELATION &&
                                      bestPitch.error() <= MAXIMUM_REPLAY_ERROR;
        return new ReplayResult((replayedYaw ? 1 : 0) + (replayedPitch ? 1 : 0),
                                bestYaw.correlation(),
                                bestYaw.error(),
                                bestYaw.reversed(),
                                bestPitch.correlation(),
                                bestPitch.error(),
                                bestPitch.reversed());
    }

    /**
     * Clears all stored interaction fingerprints.
     */
    public synchronized void clear()
    {
        for (Deque<Entry> entries : history.values()) entries.clear();
    }

    private static boolean hasVariation(final double[] values)
    {
        double squareSum = 0D;
        for (double value : values) squareSum += value * value;
        return squareSum > Math.ulp(1D);
    }

    private static double[] resample(final double[] values)
    {
        final double[] result = new double[FINGERPRINT_LENGTH];
        if (values.length == FINGERPRINT_LENGTH) {
            System.arraycopy(values, 0, result, 0, values.length);
            return result;
        }

        for (int i = 0; i < FINGERPRINT_LENGTH; i++) {
            final double position = i * (values.length - 1D) / (FINGERPRINT_LENGTH - 1D);
            final int lower = (int) position;
            final int upper = Math.min(values.length - 1, lower + 1);
            final double fraction = position - lower;
            result[i] = values[lower] * (1D - fraction) + values[upper] * fraction;
        }
        normalize(result);
        return result;
    }

    private static void normalize(final double[] values)
    {
        double mean = 0D;
        for (double value : values) mean += value;
        mean /= values.length;

        double variance = 0D;
        for (int i = 0; i < values.length; i++) {
            values[i] -= mean;
            variance += values[i] * values[i];
        }
        final double standardDeviation = Math.sqrt(variance / values.length);
        if (standardDeviation == 0D) return;
        for (int i = 0; i < values.length; i++) values[i] /= standardDeviation;
    }

    private static Similarity bestSimilarity(final double[] first, final double[] second)
    {
        Similarity best = Similarity.none();
        for (boolean reversed : new boolean[]{false, true}) {
            for (int shift = -MAX_SHIFT; shift <= MAX_SHIFT; shift++) {
                final Similarity similarity = similarity(first, second, shift, reversed);
                if (similarity.correlation() > best.correlation() ||
                    (similarity.correlation() == best.correlation() && similarity.error() < best.error())) {
                    best = similarity;
                }
            }
        }
        return best;
    }

    private static Similarity similarity(final double[] first,
                                         final double[] second,
                                         final int shift,
                                         final boolean reversed)
    {
        final int firstStart = Math.max(0, -shift);
        final int secondStart = Math.max(0, shift);
        final int length = first.length - Math.abs(shift);
        if (length < first.length - MAX_SHIFT) return Similarity.none();

        double firstMean = 0D;
        double secondMean = 0D;
        for (int i = 0; i < length; i++) {
            firstMean += first[firstStart + i];
            secondMean += sample(second, secondStart + i, reversed);
        }
        firstMean /= length;
        secondMean /= length;

        double covariance = 0D;
        double firstVariance = 0D;
        double secondVariance = 0D;
        for (int i = 0; i < length; i++) {
            final double centeredFirst = first[firstStart + i] - firstMean;
            final double centeredSecond = sample(second, secondStart + i, reversed) - secondMean;
            covariance += centeredFirst * centeredSecond;
            firstVariance += centeredFirst * centeredFirst;
            secondVariance += centeredSecond * centeredSecond;
        }

        final double denominator = Math.sqrt(firstVariance * secondVariance);
        if (denominator == 0D) return Similarity.none();
        final double signedCorrelation = covariance / denominator;
        final double sign = signedCorrelation < 0D ? -1D : 1D;

        double squaredError = 0D;
        for (int i = 0; i < length; i++) {
            final double difference = (first[firstStart + i] - firstMean) -
                                      sign * (sample(second, secondStart + i, reversed) - secondMean);
            squaredError += difference * difference;
        }
        return new Similarity(Math.abs(signedCorrelation), Math.sqrt(squaredError / length), reversed);
    }

    private static double sample(final double[] values, final int index, final boolean reversed)
    {
        return values[reversed ? values.length - 1 - index : index];
    }

    /**
     * Result returned to the Replay submodule.
     */
    public record ReplayResult(int repeatedAxes,
                               double yawCorrelation,
                               double yawError,
                               boolean yawReversed,
                               double pitchCorrelation,
                               double pitchError,
                               boolean pitchReversed) {
    }

    private record Entry(long lastSequence,
                         boolean yawEligible,
                         boolean pitchEligible,
                         double[] yawFingerprint,
                         double[] pitchFingerprint) {
    }

    private record Similarity(double correlation, double error, boolean reversed) {
        private static Similarity none()
        {
            return new Similarity(0D, Double.POSITIVE_INFINITY, false);
        }
    }
}
