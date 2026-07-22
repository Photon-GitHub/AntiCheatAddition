package de.photon.anticheataddition.modules.checks.targeting;

/**
 * Detects cross-window switching between different suspicious targeting strategies.
 *
 * <p>The specialized submodules deliberately use independent evidence counters. Without an additional combined view,
 * a client could alternate between, for example, randomized and periodic offsets so each individual counter decays
 * before reaching its threshold. This analysis only considers windows which another enabled submodule already regards
 * as suspicious, then requires several such windows, at least two different categories, and repeated transitions.</p>
 */
public final class TargetingMixedAnalysis
{
    public static final int HISTORY_LENGTH = 12;

    private static final int MINIMUM_SUSPICIOUS_OBSERVATIONS = 5;
    private static final int MINIMUM_DISTINCT_MODES = 2;
    private static final int MINIMUM_MODE_TRANSITIONS = 2;

    private TargetingMixedAnalysis()
    {
    }

    /**
     * Creates the compact mode mask stored in {@link de.photon.anticheataddition.user.data.subdata.TargetingData}.
     */
    public static int modeMask(final boolean noise,
                               final boolean precision,
                               final boolean pattern,
                               final boolean switching,
                               final boolean discontinuity)
    {
        int mask = 0;
        if (noise) mask |= Mode.NOISE.bit();
        if (precision) mask |= Mode.PRECISION.bit();
        if (pattern) mask |= Mode.PATTERN.bit();
        if (switching) mask |= Mode.SWITCHING.bit();
        if (discontinuity) mask |= Mode.DISCONTINUITY.bit();
        return mask;
    }

    /**
     * Evaluates a chronological history of zero or more suspicious-mode masks.
     */
    public static Result analyze(final int[] history)
    {
        if (history == null) throw new NullPointerException("history must not be null");
        if (history.length == 0) return new Result(false, 0, 0, 0, 0);

        int suspiciousObservations = 0;
        int observedModes = 0;
        int transitions = 0;
        int previousSuspiciousMask = 0;
        for (int modeMask : history) {
            if (modeMask == 0) continue;
            suspiciousObservations++;
            observedModes |= modeMask;
            if (previousSuspiciousMask != 0 && previousSuspiciousMask != modeMask) transitions++;
            previousSuspiciousMask = modeMask;
        }

        final int distinctModes = Integer.bitCount(observedModes);
        final boolean suspicious = suspiciousObservations >= MINIMUM_SUSPICIOUS_OBSERVATIONS &&
                                   distinctModes >= MINIMUM_DISTINCT_MODES &&
                                   transitions >= MINIMUM_MODE_TRANSITIONS;
        return new Result(suspicious,
                          suspiciousObservations,
                          distinctModes,
                          transitions,
                          observedModes);
    }

    /**
     * Categories which can participate in a cross-window mixed-strategy sequence.
     */
    public enum Mode
    {
        NOISE(1),
        PRECISION(1 << 1),
        PATTERN(1 << 2),
        SWITCHING(1 << 3),
        DISCONTINUITY(1 << 4);

        private final int bit;

        Mode(final int bit)
        {
            this.bit = bit;
        }

        private int bit()
        {
            return bit;
        }
    }

    /**
     * Metrics for the recent mixed-strategy history.
     */
    public record Result(boolean suspicious,
                         int suspiciousObservations,
                         int distinctModes,
                         int transitions,
                         int observedModeMask)
    {
    }
}
