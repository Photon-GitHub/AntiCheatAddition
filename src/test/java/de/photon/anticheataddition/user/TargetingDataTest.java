package de.photon.anticheataddition.user;

import de.photon.anticheataddition.modules.checks.targeting.TargetingContext;
import de.photon.anticheataddition.user.data.subdata.TargetingData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class TargetingDataTest
{
    @Test
    public void initializesFromSeparatePositionAndRotationPacketsInEitherOrder()
    {
        for (boolean positionFirst : new boolean[]{true, false}) {
            final TargetingData data = new TargetingData();
            assertFalse(data.addMovement(12D, 64D, -8D, 270D, 15D,
                                         positionFirst, !positionFirst, TICK_NANOS).accepted());
            assertEquals(0, data.size());
            assertTrue(data.addMovement(12D, 64D, -8D, 270D, 15D,
                                        !positionFirst, positionFirst, 2L * TICK_NANOS).accepted());
            for (int i = 0; i < 7; i++) data.addUnchangedRotation((i + 3L) * TICK_NANOS);
            final var snapshot = data.takeAcquisitionSnapshot(10L * TICK_NANOS).orElseThrow();
            assertEquals(8, snapshot.x().length);
            assertEquals(12D, snapshot.x()[0]);
            assertEquals(64D, snapshot.y()[0]);
            assertEquals(-8D, snapshot.z()[0]);
            assertEquals(-90D, snapshot.yaw()[0]);
            assertEquals(15D, snapshot.pitch()[0]);
        }
    }

    @Test
    public void malformedInitialComponentsCannotSupplyMissingState()
    {
        final TargetingData data = new TargetingData();
        assertFalse(data.addMovement(1D, 64D, 2D, Double.NaN, 0D, true, true, TICK_NANOS).accepted());
        assertFalse(data.addUnchangedRotation(2L * TICK_NANOS).accepted());
        assertEquals(0, data.size());
        assertTrue(data.addMovement(0D, 0D, 0D, 30D, 10D, false, true, 3L * TICK_NANOS).accepted());
        assertEquals(30D, data.nearestRotation(3L * TICK_NANOS, 0L).orElseThrow().yaw());
    }

    private static final long TICK_NANOS = 50_000_000L;

    @Test
    public void storesUnchangedRotationsForPrecisionAnalysis()
    {
        final TargetingData data = new TargetingData();
        for (int i = 0; i < 36; i++) data.addRotation(20D, 10D, (i + 1L) * TICK_NANOS);

        assertEquals(36, data.size());
        assertTrue(data.takeSnapshot().isPresent());
    }


    @Test
    public void rotationlessMovementPacketsRepeatTheLastRotation()
    {
        final TargetingData data = new TargetingData();
        assertFalse(data.addUnchangedRotation(TICK_NANOS).accepted());

        data.addRotation(20D, 10D, 2L * TICK_NANOS);
        for (int i = 0; i < 31; i++) data.addUnchangedRotation((3L + i) * TICK_NANOS);

        final TargetingData.Snapshot snapshot = data.takeSnapshot().orElseThrow();
        for (double yaw : snapshot.yaw()) assertEquals(20D, yaw);
        for (double pitch : snapshot.pitch()) assertEquals(10D, pitch);
    }

    @Test
    public void includesSequenceRangeInSnapshot()
    {
        final TargetingData.Snapshot snapshot = populatedData().takeSnapshot().orElseThrow();
        assertEquals(1L, snapshot.firstSequence());
        assertEquals(48L, snapshot.lastSequence());
    }

    @Test
    public void requiresEnoughNewPacketsBeforeAnotherSnapshot()
    {
        final TargetingData data = populatedData();
        final long currentTime = 48L * TICK_NANOS;

        assertTrue(data.takeSnapshot().isPresent());
        assertFalse(data.takeSnapshot().isPresent());

        for (int i = 0; i < 7; i++) data.addRotation(40D + i, 5D, currentTime + (i + 1L) * TICK_NANOS);
        assertFalse(data.takeSnapshot().isPresent());

        data.addRotation(47D, 5D, currentTime + 8L * TICK_NANOS);
        assertTrue(data.takeSnapshot().isPresent());
    }

    @Test
    public void retainsHistoryAcrossLargePacketGap()
    {
        final TargetingData data = populatedData();
        final TargetingData.RotationUpdate update = data.addRotation(35D,
                                                                     5D,
                                                                     48L * TICK_NANOS + 2_000_000_000L);

        assertTrue(update.accepted());
        assertEquals(49, data.size());
        assertTrue(data.takeSnapshot().isPresent());
    }

    @Test
    public void trustedRotationPreservesHistoryAndMarksOnlyItsTransition()
    {
        final TargetingData data = populatedData();
        final TargetingData.RotationUpdate update = data.addTrustedRotation(-120D,
                                                                            30D,
                                                                            49L * TICK_NANOS);

        assertTrue(update.accepted());
        assertEquals(48, data.size());
        assertTrue(data.hasPendingTrustedBoundary());

        data.addRotation(-119.5D, 29.8D, 50L * TICK_NANOS);
        assertFalse(data.hasPendingTrustedBoundary());
        assertEquals(49, data.size());
        final TargetingData.Snapshot snapshot = data.takeSnapshot().orElseThrow();
        final boolean[] trustedBreaks = snapshot.trustedBreakBefore();
        assertTrue(trustedBreaks[trustedBreaks.length - 1]);
        for (int i = 0; i < trustedBreaks.length - 1; i++) assertFalse(trustedBreaks[i]);
    }

    @Test
    public void repeatedTrustedRotationsDoNotCreateStatisticalSamples()
    {
        final TargetingData data = populatedData();
        for (int i = 0; i < 100; i++) {
            data.addTrustedRotation(i % 2 == 0 ? 90D : -90D,
                                    i % 3 == 0 ? 30D : -30D,
                                    (49L + i) * TICK_NANOS);
        }

        assertEquals(48, data.size());
        assertTrue(data.hasPendingTrustedBoundary());
    }

    @Test
    public void retainsHistoryAcrossAbruptRotationDiscontinuity()
    {
        final TargetingData data = populatedData();
        final TargetingData.RotationUpdate update = data.addRotation(170D, -70D, 49L * TICK_NANOS);

        assertTrue(update.accepted());
        assertEquals(49, data.size());
        assertTrue(data.takeSnapshot().isPresent());
    }

    @Test
    public void invalidRotationsDoNotEraseFiniteHistory()
    {
        final TargetingData data = populatedData();
        final TargetingData.RotationUpdate update = data.addRotation(Double.NaN, 5D, 49L * TICK_NANOS);

        assertTrue(update.accepted());
        assertEquals(49, data.size());
        assertTrue(data.takeSnapshot().isPresent());
    }

    @Test
    public void impossiblePitchDoesNotEraseFiniteHistory()
    {
        final TargetingData data = populatedData();
        final TargetingData.RotationUpdate update = data.addRotation(20D, Double.MAX_VALUE, 49L * TICK_NANOS);

        assertTrue(update.accepted());
        assertEquals(49, data.size());
        assertTrue(data.takeSnapshot().isPresent());
    }

    @Test
    public void clearPreservesMonotonicSequence()
    {
        final TargetingData data = populatedData();
        data.clear();
        for (int i = 0; i < 32; i++) data.addRotation(i, 0D, (100L + i) * TICK_NANOS);

        final TargetingData.Snapshot snapshot = data.takeSnapshot().orElseThrow();
        assertEquals(49L, snapshot.firstSequence());
        assertEquals(80L, snapshot.lastSequence());
    }

    @Test
    public void mixedModeHistorySurvivesRotationClear()
    {
        final TargetingData data = new TargetingData();
        data.addMixedModeObservation(TargetingContext.COMBAT, 1);
        data.clear();
        final int[] history = data.addMixedModeObservation(TargetingContext.COMBAT, 4);

        assertEquals(2, history.length);
        assertEquals(1, history[0]);
        assertEquals(4, history[1]);
    }

    @Test
    public void mixedModeHistoryIsSeparatedByContext()
    {
        final TargetingData data = new TargetingData();
        data.addMixedModeObservation(TargetingContext.COMBAT, 1);
        final int[] scaffold = data.addMixedModeObservation(TargetingContext.SCAFFOLD, 4);

        assertEquals(1, scaffold.length);
        assertEquals(4, scaffold[0]);
    }

    @Test
    public void acquisitionSnapshotRetainsPacketOrderPositions()
    {
        final TargetingData data = new TargetingData();
        for (int i = 0; i < 12; i++) {
            data.addMovement(i * 0.1D,
                             64D,
                             i * -0.05D,
                             20D - i,
                             5D,
                             true,
                             true,
                             (i + 1L) * TICK_NANOS);
        }

        final TargetingData.AcquisitionSnapshot snapshot = data.takeAcquisitionSnapshot().orElseThrow();
        assertEquals(12, snapshot.x().length);
        assertEquals(0D, snapshot.x()[0]);
        assertEquals(1.1D, snapshot.x()[11]);
        assertEquals(-0.55D, snapshot.z()[11]);
    }

    @Test
    public void storesInitialPositionAndRotationTogether()
    {
        final TargetingData data = new TargetingData();
        data.addMovement(123.5D, 70D, -44.25D, 270D, -15D, true, true, TICK_NANOS);
        for (int i = 1; i < 8; i++) {
            data.addMovement(123.5D + i,
                             70D,
                             -44.25D,
                             270D,
                             -15D,
                             true,
                             true,
                             (i + 1L) * TICK_NANOS);
        }
        final TargetingData.AcquisitionSnapshot snapshot = data.takeAcquisitionSnapshot(2L * TICK_NANOS).orElseThrow();

        assertEquals(123.5D, snapshot.x()[0]);
        assertEquals(70D, snapshot.y()[0]);
        assertEquals(-44.25D, snapshot.z()[0]);
        assertEquals(-90D, snapshot.yaw()[0]);
        assertEquals(-15D, snapshot.pitch()[0]);
    }

    @Test
    public void suppressesAcquisitionDuringTrustedBoundaryWindow()
    {
        final TargetingData data = new TargetingData();
        for (int i = 0; i < 12; i++) {
            data.addMovement(i, 64D, i, 20D, 5D, true, true, (i + 1L) * TICK_NANOS);
        }
        data.addTrustedMovement(100D, 70D, 100D, 0D, 0D, 1_000_000_000L);

        assertTrue(data.isTargetingSuppressed(1_499_999_999L));
        assertFalse(data.takeAcquisitionSnapshot(1_499_999_999L).isPresent());
        assertFalse(data.isTargetingSuppressed(1_500_000_000L));
    }

    @Test
    public void resumesAfterBoundaryWithoutExposingPreBoundaryInteractionSamples()
    {
        final TargetingData data = populatedData();
        data.addTrustedMovement(100D, 70D, 100D, 0D, 0D, 1_000_000_000L);
        data.addMovement(101D, 70D, 100D, 1D, 0D, true, true, 1_100_000_000L);

        assertTrue(data.addMovement(101D, 70D, 100D, 1D, 0D, true, true, 1_700_000_000L).accepted());
    }

    @Test
    public void repeatedDamageRequiresAnIndependentAcquisitionWindow()
    {
        final TargetingData data = new TargetingData();
        for (int i = 0; i < 12; i++) {
            data.addMovement(0D, 64D, 0D, 20D - i, 5D, true, true, (i + 1L) * TICK_NANOS);
        }

        assertTrue(data.takeAcquisitionSnapshot().isPresent());
        assertFalse(data.takeAcquisitionSnapshot().isPresent());
        data.addUnchangedRotation(20L * TICK_NANOS);
        assertFalse(data.takeAcquisitionSnapshot().isPresent());
        for (int i = 0; i < 6; i++) data.addUnchangedRotation((21L + i) * TICK_NANOS);
        assertFalse(data.takeAcquisitionSnapshot().isPresent());
        data.addUnchangedRotation(27L * TICK_NANOS);
        final var next = data.takeAcquisitionSnapshot().orElseThrow();
        assertEquals(8, next.sequence().length);
        assertEquals(13L, next.sequence()[0]);
        assertEquals(20L, next.sequence()[7]);
    }

    @Test
    public void trustedBoundaryRequiresEnoughPostTeleportAcquisitionSamples()
    {
        final TargetingData data = new TargetingData();
        for (int i = 0; i < 12; i++) {
            data.addMovement(0D, 64D, 0D, 20D - i, 5D, true, true, (i + 1L) * TICK_NANOS);
        }
        data.addTrustedMovement(100D, 70D, 100D, 0D, 0D, 20L * TICK_NANOS);
        for (int i = 0; i < 7; i++) {
            data.addMovement(100D, 70D, 100D, i, 0D, true, true, (21L + i) * TICK_NANOS);
        }
        assertFalse(data.takeAcquisitionSnapshot().isPresent());
        data.addMovement(100D, 70D, 100D, 8D, 0D, true, true, 30L * TICK_NANOS);
        assertTrue(data.takeAcquisitionSnapshot().isPresent());
    }

    private static TargetingData populatedData()
    {
        final TargetingData data = new TargetingData();
        for (int i = 0; i < 48; i++) {
            data.addRotation(10D + i * 0.5D, 5D + i * 0.02D, (i + 1L) * TICK_NANOS);
        }
        return data;
    }
}
