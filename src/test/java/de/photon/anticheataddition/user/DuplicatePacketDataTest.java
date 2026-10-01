package de.photon.anticheataddition.user;

import de.photon.anticheataddition.user.data.subdata.ChallengeReplyData;
import de.photon.anticheataddition.user.data.subdata.SlotSelectionData;
import de.photon.anticheataddition.user.data.subdata.TickPacketData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class DuplicatePacketDataTest
{
    @Test
    public void omittedTickMarkersDoNotSuppressDetectionOrFurtherReports()
    {
        final TickPacketData data = new TickPacketData();
        assertFalse(data.recordPacket(1L));
        assertFalse(data.recordPacket(2L));
        assertFalse(data.recordPacket(3L));
        assertTrue(data.recordPacket(4L));
        assertFalse(data.recordPacket(5L));
        assertTrue(data.recordPacket(1_000_000_004L));
    }

    @Test
    public void emptyMarkersAndAlternatingCleanTicksCannotEraseEvidence()
    {
        final TickPacketData data = new TickPacketData();
        for (int duplicate = 0; duplicate < 3; duplicate++) {
            assertFalse(data.recordPacket(duplicate * 100_000_000L + 1));
            assertEquals(duplicate == 2, data.recordPacket(duplicate * 100_000_000L + 2));
            for (int empty = 0; empty < 1000; empty++) data.endTick();
            assertFalse(data.recordPacket(duplicate * 100_000_000L + 3));
            data.endTick();
        }
    }

    @Test
    public void reportCooldownSurvivesForgedTickMarkers()
    {
        final TickPacketData data = new TickPacketData();
        data.recordPacket(1L);
        data.recordPacket(2L);
        data.recordPacket(3L);
        assertTrue(data.recordPacket(4L));
        data.endTick();
        assertFalse(data.recordPacket(5L));
        assertFalse(data.recordPacket(6L));
        assertTrue(data.recordPacket(1_000_000_004L));
    }

    @Test
    public void quietServerTimeAndTrustedResetAllowRecovery()
    {
        final TickPacketData data = new TickPacketData();
        data.recordPacket(1L);
        data.recordPacket(2L);
        data.recordPacket(3L);
        assertFalse(data.recordPacket(10_000_000_003L));
        assertFalse(data.recordPacket(10_000_000_004L));
        assertTrue(data.recordPacket(10_000_000_005L));

        data.reset();
        assertFalse(data.recordPacket(10_000_000_006L));
        assertFalse(data.recordPacket(10_000_000_007L));
        assertFalse(data.recordPacket(10_000_000_008L));
        assertTrue(data.recordPacket(10_000_000_009L));
    }

    @Test
    public void queuedVanillaTicksRemainValidWhenDeliveredTogether()
    {
        final TickPacketData data = new TickPacketData();
        for (int tick = 0; tick < 1000; tick++) {
            assertFalse(data.recordPacket(1L));
            data.endTick();
        }
    }

    @Test
    public void separateStreamsCannotDonateBoundariesOrFailureCredit()
    {
        final TickPacketData input = new TickPacketData();
        final TickPacketData paddles = new TickPacketData();
        for (int tick = 0; tick < 3; tick++) {
            assertFalse(input.recordPacket(tick + 1L));
            assertEquals(tick == 2, input.recordPacket(tick + 1L));
            assertFalse(paddles.recordPacket(tick + 1L));
            input.endTick();
            paddles.endTick();
        }
    }

    @Test
    public void slotCorrectionDiscardsDuplicateEvidence()
    {
        final SlotSelectionData data = new SlotSelectionData();
        assertFalse(data.record(4));
        assertTrue(data.record(4));
        assertFalse(data.record(5));
        data.reset();
        assertFalse(data.record(5));
        assertTrue(data.record(5));
    }

    @Test
    public void repliesRequireObservedChallengeAndRespectReissues()
    {
        final ChallengeReplyData data = new ChallengeReplyData();
        assertFalse(data.replied(7));
        data.issued(7);
        data.issued(7);
        assertTrue(data.replied(7));
        assertTrue(data.replied(7));
        assertFalse(data.replied(7));
        data.issued(7);
        assertTrue(data.replied(7));
        assertFalse(data.replied(7));
    }

    @Test
    public void unknownReplySpamCannotEvictEvidenceOrCreateCredit()
    {
        final ChallengeReplyData data = new ChallengeReplyData();
        data.issued(7);
        assertTrue(data.replied(7));
        data.issued(8);
        for (long id = 100; id < 10_000; id++)
            assertFalse(data.replied(id));

        assertFalse(data.replied(7));
        assertTrue(data.replied(8));
        assertFalse(data.replied(8));
    }

    @Test
    public void completedRepliesDoNotDisplaceOutstandingCredits()
    {
        final ChallengeReplyData data = new ChallengeReplyData();
        data.issued(7);
        data.issued(7);
        assertTrue(data.replied(7));
        for (long id = 100; id < 10_000; id++) {
            assertTrue(data.issued(id));
            assertTrue(data.replied(id));
        }
        assertTrue(data.replied(7));
        assertFalse(data.replied(7));
    }

    @Test
    public void fullWindowRejectsOverflowWithoutEvictingDelayedReplies()
    {
        final ChallengeReplyData data = new ChallengeReplyData();
        for (long id = 0; id < 1024; id++) assertTrue(data.issued(id));
        assertFalse(data.issued(1024));
        assertFalse(data.replied(1024));
        assertFalse(data.issued(0));
        assertTrue(data.replied(0));
        assertTrue(data.issued(1024));
        for (long id = 1; id <= 1024; id++) assertTrue(data.replied(id));
        assertFalse(data.replied(0));
    }

    @Test
    public void repeatedIdCannotOverflowTheCreditCounter()
    {
        final ChallengeReplyData data = new ChallengeReplyData();
        for (int count = 0; count < 1024; count++) assertTrue(data.issued(7));
        assertFalse(data.issued(7));
        for (int count = 0; count < 1024; count++) assertTrue(data.replied(7));
        assertFalse(data.replied(7));
        assertTrue(data.issued(7));
        assertTrue(data.replied(7));
    }

    @Test
    public void resetDiscardsCreditsAndRestoresTheWindow()
    {
        final ChallengeReplyData data = new ChallengeReplyData();
        for (long id = 0; id < 1024; id++) assertTrue(data.issued(id));
        data.reset();
        assertFalse(data.replied(0));
        for (long id = 0; id < 1024; id++) assertTrue(data.issued(id));
        assertFalse(data.issued(1024));
    }
}
