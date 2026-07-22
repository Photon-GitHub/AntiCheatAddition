package de.photon.anticheataddition.modules;

import de.photon.anticheataddition.modules.checks.targeting.TargetingContext;
import de.photon.anticheataddition.modules.checks.targeting.TargetingReversalAnalysis;
import de.photon.anticheataddition.user.data.subdata.TargetingData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class TargetingReversalAnalysisTest
{
    private static final long TICK_NANOS = 50_000_000L;

    @Test
    public void detectsOppositeDirectionYawPoisoning()
    {
        final TargetingReversalAnalysis.Result result = analyze(new double[]{0D, 1D, 2D, 102D, 2D},
                                                                new double[]{0D, 0D, 0D, 0D, 0D},
                                                                TICK_NANOS);

        assertTrue(result.yaw().suspicious());
        assertTrue(result.yaw().severe());
        assertEquals(1, result.suspiciousAxisCount());
    }

    @Test
    public void detectsReturnSplitAcrossSeveralPackets()
    {
        final TargetingReversalAnalysis.Result result = analyze(new double[]{0D, 1D, 91D, 61D, 31D, 1D},
                                                                new double[]{0D, 0D, 0D, 0D, 0D, 0D},
                                                                TICK_NANOS);

        assertTrue(result.yaw().suspicious());
        assertTrue(result.yaw().packetCount() >= 3);
    }


    @Test
    public void detectsReturnSplitAcrossMoreThanFormerThreePacketLimit()
    {
        final TargetingReversalAnalysis.Result result = analyze(new double[]{0D, 1D, 91D, 76D, 61D, 46D, 31D, 16D, 1D},
                                                                new double[]{0D, 0D, 0D, 0D, 0D, 0D, 0D, 0D, 0D},
                                                                TICK_NANOS);

        assertTrue(result.yaw().suspicious());
        assertTrue(result.yaw().packetCount() > 4);
    }

    @Test
    public void detectsRelativeReversalBelowFormerDiscontinuityFloor()
    {
        final TargetingReversalAnalysis.Result result = analyze(new double[]{0D, 0.4D, 0.8D, 24.8D, 0.8D},
                                                                new double[]{0D, 0D, 0D, 0D, 0D},
                                                                TICK_NANOS);

        assertTrue(result.yaw().suspicious());
    }

    @Test
    public void detectsSubThirtyDegreeReturnSpreadAcrossMostOfWindow()
    {
        final TargetingReversalAnalysis.Result result = analyze(
                new double[]{0D, 0.2D, 0.4D, 0.6D, 0.8D, -27.458724D, -24.632852D, -21.806980D,
                        -18.981107D, -16.155235D, -13.329362D, -10.503490D, -7.677617D,
                        -4.851745D, -2.025872D, 0.8D},
                new double[16],
                TICK_NANOS);

        assertTrue(result.yaw().suspicious());
        assertTrue(result.yaw().packetCount() > 8);
    }

    @Test
    public void detectsBothAxes()
    {
        final TargetingReversalAnalysis.Result result = analyze(new double[]{0D, 70D, 1D},
                                                                new double[]{0D, 35D, 1D},
                                                                TICK_NANOS);

        assertEquals(2, result.suspiciousAxisCount());
        assertTrue(result.evidenceWeight() >= 2);
    }

    @Test
    public void packetGapDoesNotExemptReversal()
    {
        final long[] timestamps = {TICK_NANOS, 2L * TICK_NANOS, 3_000_000_000L};
        final TargetingData.InteractionSnapshot snapshot = snapshot(new double[]{0D, 100D, 0D},
                                                                    new double[]{0D, 0D, 0D},
                                                                    timestamps);
        final TargetingReversalAnalysis.Result result = TargetingReversalAnalysis.analyze(snapshot);

        assertTrue(result.yaw().suspicious());
        assertTrue(result.yaw().elapsedNanos() > 1_000_000_000L);
    }

    @Test
    public void ignoresReversalAcrossTrustedServerBoundary()
    {
        final double[] yaw = {0D, 100D, 0D};
        final double[] pitch = {0D, 0D, 0D};
        final long[] timestamps = {TICK_NANOS, 2L * TICK_NANOS, 3L * TICK_NANOS};
        final long[] sequence = {1L, 2L, 3L};
        final boolean[] trustedBreaks = {false, false, true};
        final TargetingData.InteractionSnapshot snapshot = new TargetingData.InteractionSnapshot(
                TargetingContext.COMBAT,
                yaw,
                pitch,
                timestamps,
                sequence,
                trustedBreaks,
                timestamps[timestamps.length - 1]);

        assertEquals(0, TargetingReversalAnalysis.analyze(snapshot).suspiciousAxisCount());
    }

    @Test
    public void ignoresOrdinaryHumanCorrection()
    {
        final TargetingReversalAnalysis.Result result = analyze(new double[]{0D, 2D, 5D, 7D, 6D, 8D},
                                                                new double[]{0D, 0.5D, 1D, 1.3D, 1.1D, 1.4D},
                                                                TICK_NANOS);

        assertEquals(0, result.suspiciousAxisCount());
        assertFalse(result.yaw().suspicious());
        assertFalse(result.pitch().suspicious());
    }

    @Test
    public void ignoresLargeTurnWithoutOpposingReturn()
    {
        final TargetingReversalAnalysis.Result result = analyze(new double[]{0D, 1D, 90D, 130D},
                                                                new double[]{0D, 0D, 0D, 0D},
                                                                TICK_NANOS);

        assertEquals(0, result.suspiciousAxisCount());
    }

    private static TargetingReversalAnalysis.Result analyze(final double[] yaw,
                                                            final double[] pitch,
                                                            final long interval)
    {
        final long[] timestamps = new long[yaw.length];
        for (int i = 0; i < timestamps.length; i++) timestamps[i] = (i + 1L) * interval;
        return TargetingReversalAnalysis.analyze(snapshot(yaw, pitch, timestamps));
    }

    private static TargetingData.InteractionSnapshot snapshot(final double[] yaw,
                                                              final double[] pitch,
                                                              final long[] timestamps)
    {
        final long[] sequence = new long[yaw.length];
        for (int i = 0; i < sequence.length; i++) sequence[i] = i + 1L;
        return new TargetingData.InteractionSnapshot(TargetingContext.COMBAT,
                                                     yaw,
                                                     pitch,
                                                     timestamps,
                                                     sequence,
                                                     timestamps[timestamps.length - 1]);
    }
}
