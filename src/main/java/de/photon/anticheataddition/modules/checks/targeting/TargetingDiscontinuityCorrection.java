package de.photon.anticheataddition.modules.checks.targeting;

import java.util.Arrays;

import static de.photon.anticheataddition.util.mathematics.DataUtil.median;

/** Removes trusted boundaries and isolated level shifts before residual classification. */
final class TargetingDiscontinuityCorrection
{
    private static final double DISCONTINUITY_MAD_MULTIPLIER = 10D;

    private TargetingDiscontinuityCorrection() {}

    static double[] removeDiscontinuities(final double[] values,
                                                  final double minimumDiscontinuity,
                                                  final boolean[] trustedBreakBefore)
    {
        if (values.length != trustedBreakBefore.length) {
            throw new IllegalArgumentException("values and trustedBreakBefore must have the same length");
        }
        if (values.length < 4) return Arrays.copyOf(values, values.length);

        final double[] deltas = new double[values.length - 1];
        final double[] ordinaryAbsoluteDeltas = new double[deltas.length];
        int ordinaryAbsoluteCount = 0;
        for (int i = 0; i < deltas.length; i++) {
            deltas[i] = values[i + 1] - values[i];
            if (!trustedBreakBefore[i + 1]) ordinaryAbsoluteDeltas[ordinaryAbsoluteCount++] = Math.abs(deltas[i]);
        }

        final double medianAbsoluteDelta = ordinaryAbsoluteCount == 0
                                           ? 0D
                                           : median(Arrays.copyOf(ordinaryAbsoluteDeltas, ordinaryAbsoluteCount));
        final double[] absoluteDeviations = new double[ordinaryAbsoluteCount];
        for (int i = 0; i < ordinaryAbsoluteCount; i++) {
            absoluteDeviations[i] = Math.abs(ordinaryAbsoluteDeltas[i] - medianAbsoluteDelta);
        }
        final double medianAbsoluteDeviation = ordinaryAbsoluteCount == 0 ? 0D : median(absoluteDeviations);
        final double discontinuityThreshold = Math.max(minimumDiscontinuity,
                                                       medianAbsoluteDelta +
                                                       DISCONTINUITY_MAD_MULTIPLIER * medianAbsoluteDeviation);

        final double[] ordinaryDeltas = new double[deltas.length];
        int ordinaryDeltaCount = 0;
        for (int i = 0; i < deltas.length; i++) {
            if (!trustedBreakBefore[i + 1] && Math.abs(deltas[i]) <= discontinuityThreshold) {
                ordinaryDeltas[ordinaryDeltaCount++] = deltas[i];
            }
        }
        final double expectedDelta = ordinaryDeltaCount == 0
                                     ? 0D
                                     : median(Arrays.copyOf(ordinaryDeltas, ordinaryDeltaCount));

        final double[] corrected = new double[values.length];
        corrected[0] = values[0];
        double accumulatedCorrection = 0D;
        for (int i = 1; i < values.length; i++) {
            final double delta = values[i] - values[i - 1];
            if (trustedBreakBefore[i] || Math.abs(delta) > discontinuityThreshold) {
                final double localExpectedDelta = localExpectedDelta(deltas,
                                                                     trustedBreakBefore,
                                                                     i - 1,
                                                                     discontinuityThreshold,
                                                                     expectedDelta);
                accumulatedCorrection += delta - localExpectedDelta;
            }
            corrected[i] = values[i] - accumulatedCorrection;
        }
        return corrected;
    }

    private static double localExpectedDelta(final double[] deltas,
                                             final boolean[] trustedBreakBefore,
                                             final int discontinuityIndex,
                                             final double discontinuityThreshold,
                                             final double fallback)
    {
        final double[] nearby = new double[8];
        int count = 0;
        for (int distance = 1; distance <= 4; distance++) {
            final int before = discontinuityIndex - distance;
            if (before >= 0 &&
                !trustedBreakBefore[before + 1] &&
                Math.abs(deltas[before]) <= discontinuityThreshold) nearby[count++] = deltas[before];
            final int after = discontinuityIndex + distance;
            if (after < deltas.length &&
                !trustedBreakBefore[after + 1] &&
                Math.abs(deltas[after]) <= discontinuityThreshold) nearby[count++] = deltas[after];
        }
        return count == 0 ? fallback : median(Arrays.copyOf(nearby, count));
    }

}
