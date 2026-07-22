package de.photon.anticheataddition.modules.checks.targeting;

import java.util.Arrays;

/**
 * Pure analysis for repeated, oscillating large rotations.
 *
 * <p>A single fast turn or flick is not suspicious. This analysis requires several robustly unusual deltas which also
 * reverse direction and cancel much of their travelled path. That combination covers attempts to keep invalidating a
 * statistical window by jumping away from and back towards the real target without treating ordinary fast camera
 * movement as a violation.</p>
 *
 * <p>The absolute floors only decide whether a delta is routed to this analysis. Every delta at or above the floor is
 * retained, even when a client deliberately makes all jumps large enough to inflate robust baseline statistics.
 * Smaller rotations remain in the ordinary residual, periodicity, switching, and interaction-reversal checks, so
 * being just below a floor is not an exemption.</p>
 */
public final class TargetingDiscontinuityAnalysis
{
    public static final int MINIMUM_SAMPLE_COUNT = 32;

    private static final double YAW_ABSOLUTE_FLOOR = 25D;
    private static final double PITCH_ABSOLUTE_FLOOR = 15D;
    private static final double ROBUST_DEVIATION_MULTIPLIER = 8D;

    private static final int MINIMUM_DISCONTINUITIES = 3;
    private static final int STRONG_FULL_WINDOW_DISCONTINUITIES = 5;
    private static final double MINIMUM_DIRECTION_CHANGE_RATIO = 0.45D;
    private static final double MAXIMUM_PATH_EFFICIENCY = 0.6D;
    private static final double MINIMUM_REVERSAL_ENERGY = 0.22D;
    private static final int SEVERE_DISCONTINUITIES = 6;

    private TargetingDiscontinuityAnalysis()
    {
    }

    /**
     * Evaluates yaw and pitch independently over the full and overlapping 32-sample windows.
     */
    public static Result analyze(final double[] yaw, final double[] pitch)
    {
        if (yaw == null || pitch == null) throw new NullPointerException("yaw and pitch must not be null");
        return analyze(yaw, pitch, new boolean[yaw.length]);
    }

    /**
     * Evaluates yaw and pitch while excluding server-authoritative transitions from player-generated evidence.
     */
    public static Result analyze(final double[] yaw,
                                 final double[] pitch,
                                 final boolean[] trustedBreakBefore)
    {
        if (yaw == null || pitch == null || trustedBreakBefore == null) {
            throw new NullPointerException("yaw, pitch and trustedBreakBefore must not be null");
        }
        if (yaw.length != pitch.length || yaw.length != trustedBreakBefore.length) {
            throw new IllegalArgumentException("all sample arrays must have the same length");
        }
        if (yaw.length < MINIMUM_SAMPLE_COUNT) throw new IllegalArgumentException("not enough rotation samples");

        validateFinite(yaw);
        validateFinite(pitch);

        final AxisWindow fullYaw = analyzeWindow(yaw, true, YAW_ABSOLUTE_FLOOR, trustedBreakBefore);
        final AxisWindow fullPitch = analyzeWindow(pitch, false, PITCH_ABSOLUTE_FLOOR, trustedBreakBefore);
        final boolean[] earlyBreaks = Arrays.copyOfRange(trustedBreakBefore, 0, MINIMUM_SAMPLE_COUNT);
        final AxisWindow earlyYaw = analyzeWindow(Arrays.copyOfRange(yaw, 0, MINIMUM_SAMPLE_COUNT),
                                                  true,
                                                  YAW_ABSOLUTE_FLOOR,
                                                  earlyBreaks);
        final AxisWindow earlyPitch = analyzeWindow(Arrays.copyOfRange(pitch, 0, MINIMUM_SAMPLE_COUNT),
                                                    false,
                                                    PITCH_ABSOLUTE_FLOOR,
                                                    earlyBreaks);
        final int recentStart = yaw.length - MINIMUM_SAMPLE_COUNT;
        final boolean[] recentBreaks = Arrays.copyOfRange(trustedBreakBefore, recentStart, trustedBreakBefore.length);
        recentBreaks[0] = false;
        final AxisWindow recentYaw = analyzeWindow(Arrays.copyOfRange(yaw, recentStart, yaw.length),
                                                   true,
                                                   YAW_ABSOLUTE_FLOOR,
                                                   recentBreaks);
        final AxisWindow recentPitch = analyzeWindow(Arrays.copyOfRange(pitch, recentStart, pitch.length),
                                                     false,
                                                     PITCH_ABSOLUTE_FLOOR,
                                                     recentBreaks);

        return new Result(combine(fullYaw, earlyYaw, recentYaw),
                          combine(fullPitch, earlyPitch, recentPitch));
    }

    private static AxisResult combine(final AxisWindow full,
                                      final AxisWindow early,
                                      final AxisWindow recent)
    {
        int suspiciousWindows = 0;
        AxisWindow representative = full;
        for (AxisWindow window : new AxisWindow[]{full, early, recent}) {
            if (!window.suspicious()) continue;
            suspiciousWindows++;
            if (window.confidence() > representative.confidence()) representative = window;
        }

        final boolean suspicious = suspiciousWindows >= 2 ||
                                   full.severe() ||
                                   full.discontinuityCount() >= STRONG_FULL_WINDOW_DISCONTINUITIES &&
                                   full.pathEfficiency() <= MAXIMUM_PATH_EFFICIENCY;
        if (!suspicious) return AxisResult.natural(full.threshold(), full.maximumDelta());

        return new AxisResult(true,
                              representative.severe() || full.severe(),
                              representative.discontinuityCount(),
                              representative.directionChangeRatio(),
                              representative.pathEfficiency(),
                              representative.reversalEnergy(),
                              representative.maximumDelta(),
                              representative.threshold(),
                              suspiciousWindows);
    }

    private static AxisWindow analyzeWindow(final double[] rotations,
                                            final boolean yawAxis,
                                            final double absoluteFloor,
                                            final boolean[] trustedBreakBefore)
    {
        if (rotations.length != trustedBreakBefore.length) {
            throw new IllegalArgumentException("rotations and trustedBreakBefore must have the same length");
        }

        final double[] deltas = new double[rotations.length - 1];
        final double[] ordinaryAbsoluteDeltas = new double[deltas.length];
        int ordinaryCount = 0;
        for (int i = 0; i < deltas.length; i++) {
            final double delta = yawAxis
                                 ? TargetingAnalysis.signedYawDelta(rotations[i + 1], rotations[i])
                                 : rotations[i + 1] - rotations[i];
            deltas[i] = delta;
            if (!trustedBreakBefore[i + 1]) ordinaryAbsoluteDeltas[ordinaryCount++] = Math.abs(delta);
        }

        if (ordinaryCount == 0) return AxisWindow.natural(0, absoluteFloor, 0D);
        final double[] retainedAbsoluteDeltas = Arrays.copyOf(ordinaryAbsoluteDeltas, ordinaryCount);
        final double median = median(retainedAbsoluteDeltas);
        final double[] deviations = new double[retainedAbsoluteDeltas.length];
        for (int i = 0; i < retainedAbsoluteDeltas.length; i++) {
            deviations[i] = Math.abs(retainedAbsoluteDeltas[i] - median);
        }
        final double medianAbsoluteDeviation = median(deviations);
        final double threshold = Math.max(absoluteFloor,
                                          median + ROBUST_DEVIATION_MULTIPLIER * medianAbsoluteDeviation);

        AxisWindow best = AxisWindow.natural(0, threshold, 0D);
        int segmentStartDelta = 0;
        for (int sampleIndex = 1; sampleIndex <= rotations.length; sampleIndex++) {
            final boolean segmentEnd = sampleIndex == rotations.length || trustedBreakBefore[sampleIndex];
            if (!segmentEnd) continue;

            final int segmentEndDelta = sampleIndex - 1;
            final AxisWindow segment = analyzeSegment(deltas,
                                                      segmentStartDelta,
                                                      segmentEndDelta,
                                                      threshold,
                                                      absoluteFloor);
            if (segment.confidence() > best.confidence() ||
                !best.suspicious() && segment.discontinuityCount() > best.discontinuityCount()) best = segment;
            segmentStartDelta = sampleIndex;
        }
        return best;
    }

    private static AxisWindow analyzeSegment(final double[] deltas,
                                             final int fromInclusive,
                                             final int toExclusive,
                                             final double threshold,
                                             final double absoluteFloor)
    {
        final double[] discontinuities = new double[Math.max(0, toExclusive - fromInclusive)];
        int count = 0;
        double maximumDelta = 0D;
        for (int i = fromInclusive; i < toExclusive; i++) {
            final double delta = deltas[i];
            maximumDelta = Math.max(maximumDelta, Math.abs(delta));
            // The robust threshold is diagnostic only. Using it as the gate would let a client make every packet a
            // differently sized large jump and inflate the threshold above its own malicious rotations.
            if (Math.abs(delta) >= absoluteFloor) discontinuities[count++] = delta;
        }
        if (count < MINIMUM_DISCONTINUITIES) return AxisWindow.natural(count, threshold, maximumDelta);

        double pathLength = 0D;
        double netMovement = 0D;
        double reversalEnergy = 0D;
        int directionChanges = 0;
        for (int i = 0; i < count; i++) {
            final double delta = discontinuities[i];
            pathLength += Math.abs(delta);
            netMovement += delta;
            if (i == 0 || discontinuities[i - 1] * delta >= 0D) continue;
            directionChanges++;
            reversalEnergy += Math.min(Math.abs(discontinuities[i - 1]), Math.abs(delta));
        }

        final double directionChangeRatio = directionChanges / (double) (count - 1);
        final double pathEfficiency = pathLength == 0D ? 1D : Math.abs(netMovement) / pathLength;
        final double normalizedReversalEnergy = pathLength == 0D ? 0D : reversalEnergy / pathLength;
        final boolean suspicious = directionChangeRatio >= MINIMUM_DIRECTION_CHANGE_RATIO &&
                                   pathEfficiency <= MAXIMUM_PATH_EFFICIENCY &&
                                   normalizedReversalEnergy >= MINIMUM_REVERSAL_ENERGY;
        final boolean severe = suspicious &&
                               (count >= SEVERE_DISCONTINUITIES || maximumDelta >= absoluteFloor * 3D);
        final double confidence = count * directionChangeRatio * (1D - pathEfficiency) *
                                  Math.max(0D, normalizedReversalEnergy);
        return new AxisWindow(suspicious,
                              severe,
                              count,
                              directionChangeRatio,
                              pathEfficiency,
                              normalizedReversalEnergy,
                              maximumDelta,
                              threshold,
                              confidence);
    }

    private static double median(final double[] values)
    {
        final double[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        final int middle = sorted.length / 2;
        return (sorted.length & 1) == 1 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) * 0.5D;
    }

    private static void validateFinite(final double[] values)
    {
        for (double value : values) {
            if (!Double.isFinite(value)) throw new IllegalArgumentException("rotation samples must be finite");
        }
    }

    /**
     * Combined yaw and pitch discontinuity result.
     */
    public record Result(AxisResult yaw, AxisResult pitch)
    {
        public int suspiciousAxisCount()
        {
            return (yaw.suspicious() ? 1 : 0) + (pitch.suspicious() ? 1 : 0);
        }

        public int evidenceWeight()
        {
            return suspiciousAxisCount() + (yaw.severe() ? 1 : 0) + (pitch.severe() ? 1 : 0);
        }
    }

    /**
     * Metrics for one axis.
     */
    public record AxisResult(boolean suspicious,
                             boolean severe,
                             int discontinuityCount,
                             double directionChangeRatio,
                             double pathEfficiency,
                             double reversalEnergy,
                             double maximumDelta,
                             double threshold,
                             int suspiciousWindowCount)
    {
        private static AxisResult natural(final double threshold, final double maximumDelta)
        {
            return new AxisResult(false, false, 0, 0D, 1D, 0D, maximumDelta, threshold, 0);
        }
    }

    private record AxisWindow(boolean suspicious,
                              boolean severe,
                              int discontinuityCount,
                              double directionChangeRatio,
                              double pathEfficiency,
                              double reversalEnergy,
                              double maximumDelta,
                              double threshold,
                              double confidence)
    {
        private static AxisWindow natural(final int discontinuityCount,
                                          final double threshold,
                                          final double maximumDelta)
        {
            return new AxisWindow(false,
                                  false,
                                  discontinuityCount,
                                  0D,
                                  1D,
                                  0D,
                                  maximumDelta,
                                  threshold,
                                  0D);
        }
    }
}
