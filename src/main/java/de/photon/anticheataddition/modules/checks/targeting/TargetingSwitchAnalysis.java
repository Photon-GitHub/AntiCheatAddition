package de.photon.anticheataddition.modules.checks.targeting;

import java.util.Arrays;

/**
 * Detects deliberate switching between clean and randomized targeting sections.
 *
 * <p>This is kept separate from {@link TargetingAnalysis} because it evaluates variance changes between short segments
 * rather than the distribution of individual residuals. It is mainly intended for clients which alternate axes or add
 * clean packets to make the complete statistical window fail a stationarity check.</p>
 */
public final class TargetingSwitchAnalysis
{
    private static final double MAXIMUM_QUIET_RMS = 0.25D;
    private static final double MINIMUM_ACTIVE_RMS = 1D;
    private static final double MINIMUM_VARIANCE_CONTRAST = 5D;
    private static final int MINIMUM_ACTIVE_SEGMENTS = 2;
    private static final int MINIMUM_QUIET_SEGMENTS = 1;
    private static final int MINIMUM_STATE_TRANSITIONS = 1;
    private static final double MINIMUM_SIGN_CHANGE_RATIO = 0.34D;
    private static final double MINIMUM_PERMUTATION_ENTROPY = 0.72D;

    private TargetingSwitchAnalysis()
    {
    }

    /**
     * Evaluates variance switching independently on yaw and pitch.
     */
    public static Result analyze(final TargetingAnalysis.Result result)
    {
        if (result == null) throw new NullPointerException("result must not be null");
        return new Result(analyzeAxis(result.yawFingerprint(), result.yaw()),
                          analyzeAxis(result.pitchFingerprint(), result.pitch()));
    }

    private static AxisResult analyzeAxis(final double[] fingerprint,
                                          final TargetingAnalysis.AxisResult analysis)
    {
        // The fingerprint is normalized, so an absolute amplitude floor would only create a public
        // white-box bypass. Precision windows already produce an all-zero fingerprint.
        if (analysis.signChangeRatio() < MINIMUM_SIGN_CHANGE_RATIO ||
            analysis.permutationEntropy() < MINIMUM_PERMUTATION_ENTROPY) return AxisResult.natural();

        final int segmentCount = fingerprint.length >= 42 ? 6 : 4;
        final double[] segmentRms = new double[segmentCount];
        for (int segment = 0; segment < segmentCount; segment++) {
            final int from = segment * fingerprint.length / segmentCount;
            final int to = (segment + 1) * fingerprint.length / segmentCount;
            double squareSum = 0D;
            for (int i = from; i < to; i++) squareSum += fingerprint[i] * fingerprint[i];
            segmentRms[segment] = Math.sqrt(squareSum / (to - from));
        }

        double minimum = segmentRms[0];
        double maximum = segmentRms[0];
        int activeSegments = 0;
        int quietSegments = 0;
        int transitions = 0;
        int previousState = 0;
        for (double rms : segmentRms) {
            minimum = Math.min(minimum, rms);
            maximum = Math.max(maximum, rms);
            final int state = rms <= MAXIMUM_QUIET_RMS ? -1 : rms >= MINIMUM_ACTIVE_RMS ? 1 : 0;
            if (state < 0) quietSegments++;
            else if (state > 0) activeSegments++;
            if (state != 0) {
                if (previousState != 0 && previousState != state) transitions++;
                previousState = state;
            }
        }

        final double[] sorted = Arrays.copyOf(segmentRms, segmentRms.length);
        Arrays.sort(sorted);
        final double median = (sorted[(sorted.length - 1) / 2] + sorted[sorted.length / 2]) * 0.5D;
        final double varianceContrast = maximum / Math.max(0.05D, minimum);
        final boolean switching = varianceContrast >= MINIMUM_VARIANCE_CONTRAST &&
                                  activeSegments >= MINIMUM_ACTIVE_SEGMENTS &&
                                  quietSegments >= MINIMUM_QUIET_SEGMENTS &&
                                  transitions >= MINIMUM_STATE_TRANSITIONS;
        return new AxisResult(switching,
                              varianceContrast,
                              minimum,
                              maximum,
                              median,
                              activeSegments,
                              quietSegments,
                              transitions);
    }

    /**
     * Combined switching result.
     */
    public record Result(AxisResult yaw, AxisResult pitch)
    {
        public int switchingAxisCount()
        {
            return (yaw.switching() ? 1 : 0) + (pitch.switching() ? 1 : 0);
        }
    }

    /**
     * Segment-variance metrics for one axis.
     */
    public record AxisResult(boolean switching,
                             double varianceContrast,
                             double minimumSegmentRms,
                             double maximumSegmentRms,
                             double medianSegmentRms,
                             int activeSegments,
                             int quietSegments,
                             int stateTransitions)
    {
        private static AxisResult natural()
        {
            return new AxisResult(false, 1D, 0D, 0D, 0D, 0, 0, 0);
        }
    }
}
