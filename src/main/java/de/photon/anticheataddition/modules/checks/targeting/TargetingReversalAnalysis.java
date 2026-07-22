package de.photon.anticheataddition.modules.checks.targeting;

import de.photon.anticheataddition.user.data.subdata.TargetingData;

import java.util.Arrays;

/**
 * Detects interaction-centered rotation reversals which are too abrupt to be useful as ordinary target corrections.
 *
 * <p>The analysis considers every pair of adjoining cumulative segments available in the interaction history rather
 * than only individual packets. Consequently, splitting a reversal over several packets does not create a fixed
 * packet-count bypass. There is no maximum accepted rotation: larger reversals only increase confidence.</p>
 */
public final class TargetingReversalAnalysis
{
    private static final double YAW_ABSOLUTE_MINIMUM = 30D;
    private static final double YAW_RELATIVE_MINIMUM = 8D;
    private static final double YAW_TYPICAL_FLOOR = 0.35D;
    private static final double YAW_SEVERE_MAGNITUDE = 90D;

    private static final double PITCH_ABSOLUTE_MINIMUM = 18D;
    private static final double PITCH_RELATIVE_MINIMUM = 5D;
    private static final double PITCH_TYPICAL_FLOOR = 0.25D;
    private static final double PITCH_SEVERE_MAGNITUDE = 45D;

    private static final double MINIMUM_CANCELLATION_RATIO = 0.55D;
    private static final double SEVERE_CANCELLATION_RATIO = 0.8D;
    private static final double MINIMUM_RELATIVE_STRENGTH = 12D;
    private static final double SEVERE_RELATIVE_STRENGTH = 25D;

    private TargetingReversalAnalysis()
    {
    }

    /**
     * Evaluates yaw and pitch reversals ending at the interaction rotation.
     */
    public static Result analyze(final TargetingData.InteractionSnapshot snapshot)
    {
        if (snapshot == null) throw new NullPointerException("snapshot must not be null");
        final double[] snapshotYaw = snapshot.yaw();
        final double[] snapshotPitch = snapshot.pitch();
        final long[] snapshotTimestamps = snapshot.timestamp();
        final boolean[] snapshotTrustedBreaks = snapshot.trustedBreakBefore();
        if (snapshotYaw.length != snapshotPitch.length ||
            snapshotYaw.length != snapshotTimestamps.length ||
            snapshotYaw.length != snapshotTrustedBreaks.length) {
            throw new IllegalArgumentException("interaction snapshot arrays must have the same length");
        }

        final int segmentStart = latestTrustedSegmentStart(snapshotTrustedBreaks);
        final double[] yawSamples = Arrays.copyOfRange(snapshotYaw, segmentStart, snapshotYaw.length);
        final double[] pitchSamples = Arrays.copyOfRange(snapshotPitch, segmentStart, snapshotPitch.length);
        final long[] timestamps = Arrays.copyOfRange(snapshotTimestamps, segmentStart, snapshotTimestamps.length);

        final AxisResult yaw = analyzeAxis(yawSamples,
                                           timestamps,
                                           true,
                                           YAW_ABSOLUTE_MINIMUM,
                                           YAW_RELATIVE_MINIMUM,
                                           YAW_TYPICAL_FLOOR,
                                           YAW_SEVERE_MAGNITUDE);
        final AxisResult pitch = analyzeAxis(pitchSamples,
                                             timestamps,
                                             false,
                                             PITCH_ABSOLUTE_MINIMUM,
                                             PITCH_RELATIVE_MINIMUM,
                                             PITCH_TYPICAL_FLOOR,
                                             PITCH_SEVERE_MAGNITUDE);
        return new Result(snapshot.context(), yaw, pitch);
    }


    private static int latestTrustedSegmentStart(final boolean[] trustedBreakBefore)
    {
        for (int i = trustedBreakBefore.length - 1; i > 0; i--) {
            if (trustedBreakBefore[i]) return i;
        }
        return 0;
    }

    private static AxisResult analyzeAxis(final double[] rotations,
                                          final long[] timestamps,
                                          final boolean yawAxis,
                                          final double absoluteMinimum,
                                          final double relativeMinimum,
                                          final double typicalFloor,
                                          final double severeMagnitude)
    {
        if (rotations.length < 3) return AxisResult.natural();

        final double[] deltas = new double[rotations.length - 1];
        final double[] absoluteDeltas = new double[deltas.length];
        for (int i = 0; i < deltas.length; i++) {
            deltas[i] = yawAxis
                        ? TargetingAnalysis.signedYawDelta(rotations[i + 1], rotations[i])
                        : rotations[i + 1] - rotations[i];
            absoluteDeltas[i] = Math.abs(deltas[i]);
        }

        // The two opposing bypass rotations can occupy a large fraction of a short interaction window. Use the
        // lower quartile as the baseline so those very rotations cannot inflate their own comparison value.
        final double typicalMagnitude = Math.max(typicalFloor, lowerQuartileMagnitude(absoluteDeltas));
        final double[] prefixSum = prefixSum(deltas);
        Candidate best = Candidate.none();
        final int end = deltas.length;
        for (int recentLength = 1; recentLength < end; recentLength++) {
            final int recentStart = end - recentLength;
            final double recentMovement = rangeSum(prefixSum, recentStart, end);
            for (int earlierLength = 1; earlierLength <= recentStart; earlierLength++) {
                final int earlierStart = recentStart - earlierLength;
                final double earlierMovement = rangeSum(prefixSum, earlierStart, recentStart);
                if (earlierMovement == 0D || recentMovement == 0D || earlierMovement * recentMovement >= 0D) continue;

                final double earlierMagnitude = Math.abs(earlierMovement);
                final double recentMagnitude = Math.abs(recentMovement);
                final double minimumMagnitude = Math.min(earlierMagnitude, recentMagnitude);
                final double totalMovement = earlierMagnitude + recentMagnitude;
                final double cancellationRatio = 1D - Math.abs(earlierMovement + recentMovement) / totalMovement;
                final double relativeStrength = minimumMagnitude / typicalMagnitude;
                final boolean meaningfulMagnitude = minimumMagnitude >= absoluteMinimum ||
                                                    minimumMagnitude >= relativeMinimum &&
                                                    relativeStrength >= MINIMUM_RELATIVE_STRENGTH;
                if (!meaningfulMagnitude || cancellationRatio < MINIMUM_CANCELLATION_RATIO) continue;

                final double confidence = cancellationRatio *
                                          Math.min(2D, relativeStrength / MINIMUM_RELATIVE_STRENGTH) *
                                          Math.min(2D, minimumMagnitude / absoluteMinimum);
                if (confidence <= best.confidence()) continue;

                final int startSample = earlierStart;
                final long elapsedNanos = Math.max(0L, timestamps[timestamps.length - 1] - timestamps[startSample]);
                final boolean severe = minimumMagnitude >= severeMagnitude ||
                                       relativeStrength >= SEVERE_RELATIVE_STRENGTH &&
                                       cancellationRatio >= SEVERE_CANCELLATION_RATIO;
                best = new Candidate(true,
                                     severe,
                                     earlierMovement,
                                     recentMovement,
                                     cancellationRatio,
                                     typicalMagnitude,
                                     relativeStrength,
                                     earlierLength + recentLength,
                                     elapsedNanos,
                                     confidence);
            }
        }

        return best.suspicious()
               ? new AxisResult(true,
                                best.severe(),
                                best.earlierMovement(),
                                best.recentMovement(),
                                best.cancellationRatio(),
                                best.typicalMagnitude(),
                                best.relativeStrength(),
                                best.packetCount(),
                                best.elapsedNanos())
               : AxisResult.natural();
    }

    private static double[] prefixSum(final double[] values)
    {
        final double[] result = new double[values.length + 1];
        for (int i = 0; i < values.length; i++) result[i + 1] = result[i] + values[i];
        return result;
    }

    private static double rangeSum(final double[] prefixSum,
                                   final int fromInclusive,
                                   final int toExclusive)
    {
        return prefixSum[toExclusive] - prefixSum[fromInclusive];
    }


    private static double lowerQuartileMagnitude(final double[] values)
    {
        final double[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        final int retainedLength = Math.max(1, (int) Math.ceil(sorted.length * 0.25D));
        return median(Arrays.copyOf(sorted, retainedLength));
    }

    private static double median(final double[] values)
    {
        final double[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        final int middle = sorted.length / 2;
        return (sorted.length & 1) == 1 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) * 0.5D;
    }

    /**
     * Combined reversal result for one interaction.
     */
    public record Result(TargetingContext context, AxisResult yaw, AxisResult pitch)
    {
        public int suspiciousAxisCount()
        {
            return (yaw.suspicious() ? 1 : 0) + (pitch.suspicious() ? 1 : 0);
        }

        /**
         * Stronger or two-axis reversals accumulate evidence faster without introducing an upper exemption threshold.
         */
        public int evidenceWeight()
        {
            return suspiciousAxisCount() + (yaw.severe() ? 1 : 0) + (pitch.severe() ? 1 : 0);
        }
    }

    /**
     * Metrics for a short opposing movement pair on one axis.
     */
    public record AxisResult(boolean suspicious,
                             boolean severe,
                             double earlierMovement,
                             double recentMovement,
                             double cancellationRatio,
                             double typicalMagnitude,
                             double relativeStrength,
                             int packetCount,
                             long elapsedNanos)
    {
        private static AxisResult natural()
        {
            return new AxisResult(false, false, 0D, 0D, 0D, 0D, 0D, 0, 0L);
        }
    }

    private record Candidate(boolean suspicious,
                             boolean severe,
                             double earlierMovement,
                             double recentMovement,
                             double cancellationRatio,
                             double typicalMagnitude,
                             double relativeStrength,
                             int packetCount,
                             long elapsedNanos,
                             double confidence)
    {
        private static Candidate none()
        {
            return new Candidate(false, false, 0D, 0D, 0D, 0D, 0D, 0, 0L, 0D);
        }
    }
}
