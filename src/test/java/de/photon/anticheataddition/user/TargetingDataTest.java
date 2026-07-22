package de.photon.anticheataddition.user;

import de.photon.anticheataddition.modules.checks.targeting.TargetingContext;
import de.photon.anticheataddition.user.data.subdata.TargetingData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class TargetingDataTest
{
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
    public void invalidRotationsDoNotExpirePendingSnapBack()
    {
        final TargetingData data = new TargetingData();
        data.addRotation(10D, 5D, TICK_NANOS);
        data.addRotation(30D, 5D, 2L * TICK_NANOS);
        data.markInteraction(TargetingContext.COMBAT, 2L * TICK_NANOS);

        for (int i = 0; i < 20; i++) {
            assertTrue(data.addRotation(Double.NaN, 5D, (3L + i) * TICK_NANOS).accepted());
        }
        assertNotNull(data.addRotation(10D, 5D, 30L * TICK_NANOS).snapBackSample());
    }

    @Test
    public void detectsDirectSnapBackSplitAcrossManyPackets()
    {
        final TargetingData data = new TargetingData();
        data.addRotation(10D, 5D, TICK_NANOS);
        data.addRotation(30D, 5D, 2L * TICK_NANOS);
        data.markInteraction(TargetingContext.COMBAT, 2L * TICK_NANOS);

        TargetingData.SnapBackSample sample = null;
        for (int i = 1; i <= 12; i++) {
            final TargetingData.RotationUpdate update = data.addRotation(30D - 20D * i / 12D,
                                                                         5D,
                                                                         (2L + i) * TICK_NANOS);
            if (update.snapBackSample() != null) sample = update.snapBackSample();
        }
        assertNotNull(sample);
        assertTrue(sample.followingPackets() >= 10);
    }

    @Test
    public void ignoresIndirectWanderingReturn()
    {
        final TargetingData data = new TargetingData();
        data.addRotation(10D, 5D, TICK_NANOS);
        data.addRotation(30D, 5D, 2L * TICK_NANOS);
        data.markInteraction(TargetingContext.COMBAT, 2L * TICK_NANOS);

        data.addRotation(60D, 5D, 3L * TICK_NANOS);
        data.addRotation(-20D, 5D, 4L * TICK_NANOS);
        assertNull(data.addRotation(10D, 5D, 5L * TICK_NANOS).snapBackSample());
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
    public void trustedRotationCancelsPendingSnapBackWithoutClearingHistory()
    {
        final TargetingData data = new TargetingData();
        data.addRotation(10D, 5D, TICK_NANOS);
        data.addRotation(30D, 5D, 2L * TICK_NANOS);
        data.markInteraction(TargetingContext.COMBAT, 2L * TICK_NANOS);

        assertNull(data.addTrustedRotation(10D, 5D, 3L * TICK_NANOS).snapBackSample());
        assertTrue(data.hasPendingTrustedBoundary());
        assertNull(data.addRotation(30D, 5D, 4L * TICK_NANOS).snapBackSample());
        assertFalse(data.hasPendingTrustedBoundary());
        assertEquals(3, data.size());
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
    public void malformedSingleAxisRetainsOnlyThatAxesLastValue()
    {
        final TargetingData data = new TargetingData();
        data.addRotation(20D, 5D, TICK_NANOS);
        data.addRotation(Double.NaN, 7D, 2L * TICK_NANOS);
        data.markInteraction(TargetingContext.COMBAT, 2L * TICK_NANOS).ifPresent(snapshot -> {
            assertEquals(20D, snapshot.yaw()[1]);
            assertEquals(7D, snapshot.pitch()[1]);
        });
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
    public void canonicalizesVeryLargeFiniteYaw()
    {
        final TargetingData data = new TargetingData();
        data.addRotation(Double.MAX_VALUE, 0D, TICK_NANOS);
        data.addRotation(-Double.MAX_VALUE, 0D, 2L * TICK_NANOS);

        final TargetingData.InteractionSnapshot snapshot = data.markInteraction(TargetingContext.COMBAT,
                                                                                2L * TICK_NANOS).orElseThrow();
        for (double yaw : snapshot.yaw()) assertTrue(Double.isFinite(yaw));
    }

    @Test
    public void detectsOneAxisSnapBack()
    {
        final TargetingData data = new TargetingData();
        data.addRotation(10D, 5D, TICK_NANOS);
        data.addRotation(11D, 5.1D, 2L * TICK_NANOS);
        data.markInteraction(TargetingContext.COMBAT, 2L * TICK_NANOS + 10_000_000L);

        final TargetingData.RotationUpdate update = data.addRotation(10.05D, 5.2D, 3L * TICK_NANOS);
        assertNotNull(update.snapBackSample());
        assertEquals(1, update.snapBackSample().suspiciousAxes());
    }

    @Test
    public void detectsBothAxisSnapBack()
    {
        final TargetingData data = new TargetingData();
        data.addRotation(10D, 5D, TICK_NANOS);
        data.addRotation(11D, 6D, 2L * TICK_NANOS);
        data.markInteraction(TargetingContext.SCAFFOLD, 2L * TICK_NANOS + 10_000_000L);

        final TargetingData.RotationUpdate update = data.addRotation(10.04D, 5.03D, 3L * TICK_NANOS);
        assertNotNull(update.snapBackSample());
        assertEquals(2, update.snapBackSample().suspiciousAxes());
    }

    @Test
    public void detectsSnapBackAfterIntermediatePacket()
    {
        final TargetingData data = new TargetingData();
        data.addRotation(10D, 5D, TICK_NANOS);
        data.addRotation(30D, 5D, 2L * TICK_NANOS);
        data.markInteraction(TargetingContext.COMBAT, 2L * TICK_NANOS);

        assertNull(data.addRotation(29.5D, 5D, 3L * TICK_NANOS).snapBackSample());
        final TargetingData.RotationUpdate update = data.addRotation(10.1D, 5D, 4L * TICK_NANOS);
        assertNotNull(update.snapBackSample());
        assertEquals(2, update.snapBackSample().followingPackets());
    }

    @Test
    public void repeatedAttacksDoNotOverwritePendingSnapBack()
    {
        final TargetingData data = new TargetingData();
        data.addRotation(10D, 5D, TICK_NANOS);
        data.addRotation(30D, 5D, 2L * TICK_NANOS);
        data.markInteraction(TargetingContext.COMBAT, 2L * TICK_NANOS);
        data.addRotation(30.1D, 5D, 3L * TICK_NANOS);
        data.markInteraction(TargetingContext.COMBAT, 3L * TICK_NANOS);

        final TargetingData.RotationUpdate update = data.addRotation(10.1D, 5D, 4L * TICK_NANOS);
        assertNotNull(update.snapBackSample());
    }

    @Test
    public void delayedReceiveTimeDoesNotCreateAnExemption()
    {
        final TargetingData data = new TargetingData();
        data.addRotation(10D, 5D, TICK_NANOS);
        data.addRotation(30D, 5D, 2L * TICK_NANOS);
        data.markInteraction(TargetingContext.COMBAT, 2L * TICK_NANOS);

        final TargetingData.RotationUpdate update = data.addRotation(10D, 5D, 2_000_000_000L);
        assertNotNull(update.snapBackSample());
    }

    @Test
    public void detectsSnapBackWhichDeliberatelyOvershootsTheStartingAngle()
    {
        final TargetingData data = new TargetingData();
        data.addRotation(10D, 5D, TICK_NANOS);
        data.addRotation(12D, 5D, 2L * TICK_NANOS);
        data.markInteraction(TargetingContext.COMBAT, 2L * TICK_NANOS);

        final TargetingData.RotationUpdate update = data.addRotation(9D, 5D, 3L * TICK_NANOS);
        assertNotNull(update.snapBackSample());
        assertEquals(1, update.snapBackSample().suspiciousAxes());
    }

    @Test
    public void ignoresOrdinaryContinuedMovement()
    {
        final TargetingData data = new TargetingData();
        data.addRotation(10D, 5D, TICK_NANOS);
        data.addRotation(11D, 5.5D, 2L * TICK_NANOS);
        data.markInteraction(TargetingContext.COMBAT, 2L * TICK_NANOS + 10_000_000L);

        final TargetingData.RotationUpdate update = data.addRotation(12D, 6D, 3L * TICK_NANOS);
        assertNull(update.snapBackSample());
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
    public void repeatedDamageWithoutMovementCannotDuplicateAnAcquisition()
    {
        final TargetingData data = new TargetingData();
        for (int i = 0; i < 12; i++) {
            data.addMovement(0D, 64D, 0D, 20D - i, 5D, true, true, (i + 1L) * TICK_NANOS);
        }

        assertTrue(data.takeAcquisitionSnapshot().isPresent());
        assertFalse(data.takeAcquisitionSnapshot().isPresent());
        data.addUnchangedRotation(20L * TICK_NANOS);
        assertTrue(data.takeAcquisitionSnapshot().isPresent());
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
