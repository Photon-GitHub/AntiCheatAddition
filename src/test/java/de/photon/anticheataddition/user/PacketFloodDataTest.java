package de.photon.anticheataddition.user;

import de.photon.anticheataddition.user.data.subdata.PacketFloodData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PacketFloodDataTest
{
    @Test
    public void toleratesConfiguredBurst()
    {
        final PacketFloodData data = new PacketFloodData();
        for (int i = 0; i < 3; i++) {
            assertFalse(data.record(1L, 1D, 3D, 1_000_000_000L).throttled());
        }
        assertTrue(data.record(1L, 1D, 3D, 1_000_000_000L).throttled());
    }

    @Test
    public void sustainedOverflowRequiresDuration()
    {
        final PacketFloodData data = new PacketFloodData();
        final long duration = 1_000_000_000L;
        data.record(1L, 0.1D, 2D, duration);
        data.record(1L, 0.1D, 2D, duration);

        assertFalse(data.record(100_000_001L, 0.1D, 2D, duration).sustained());
        assertFalse(data.record(500_000_001L, 0.1D, 2D, duration).sustained());
        assertTrue(data.record(1_100_000_001L, 0.1D, 2D, duration).sustained());
    }

    @Test
    public void refillsAndResetsIsolationState()
    {
        final PacketFloodData data = new PacketFloodData();
        data.record(1L, 1D, 1D, 1_000_000_000L);
        data.record(1L, 1D, 1D, 1_000_000_000L);
        assertTrue(data.isThrottled(1L));

        data.record(2_000_000_001L, 1D, 1D, 1_000_000_000L);
        assertFalse(data.isThrottled(2_000_000_001L));

        data.reset();
        assertFalse(data.isThrottled(2_000_000_001L));
    }

    @Test
    public void extremeBurstCannotPermanentlyThrottleTargeting()
    {
        final PacketFloodData data = new PacketFloodData();
        for (int i = 0; i < 10_000; i++) data.record(1L, 40D, 200D, 3_000_000_000L);

        assertTrue(data.isThrottled(1_000_000_001L));
        assertFalse(data.isThrottled(1_000_000_002L));
    }

    @Test
    public void firstSustainedViolationCanBeReportedImmediately()
    {
        final PacketFloodData data = new PacketFloodData();

        assertTrue(data.shouldReport(1L, 3_000_000_000L));
        assertFalse(data.shouldReport(2L, 3_000_000_000L));
    }

    @Test
    public void sustainedHighRateRemainsDepletedBetweenOccasionalAcceptedPackets()
    {
        final PacketFloodData data = new PacketFloodData();
        final long duration = 3_000_000_000L;
        boolean sustained = false;

        for (int i = 0; i < 800; i++) {
            sustained |= data.record(1L + i * 10_000_000L, 40D, 200D, duration).sustained();
        }

        assertTrue(sustained);
    }
}
