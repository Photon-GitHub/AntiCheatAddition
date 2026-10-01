package de.photon.anticheataddition.user;

import de.photon.anticheataddition.user.data.subdata.PlayerActionData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PlayerActionDataTest
{
    @Test
    public void acceptsGapsButRejectsReplayedSequences()
    {
        final PlayerActionData data = new PlayerActionData();
        assertTrue(data.observeSequence(10));
        assertTrue(data.observeSequence(17));
        assertFalse(data.observeSequence(17));
        assertFalse(data.observeSequence(16));
        assertTrue(data.observeSequence(18));
    }

    @Test
    public void rejectsNegativeSequencesWithoutChangingHistory()
    {
        final PlayerActionData data = new PlayerActionData();
        assertFalse(data.observeSequence(-1));
        assertTrue(data.observeSequence(0));
        assertFalse(data.observeSequence(-1));
        assertFalse(data.observeSequence(0));
        assertTrue(data.observeSequence(1));
    }

    @Test
    public void tracksDiggingTargetAndUseState()
    {
        final PlayerActionData data = new PlayerActionData();
        data.startDigging(1, 64, -2);
        assertEquals(PlayerActionData.TransitionResult.VALID, data.finishDigging(1, 64, -2));
        assertEquals(PlayerActionData.TransitionResult.VALID, data.finishDigging(1, 64, -2));

        data.startUse();
        assertEquals(PlayerActionData.TransitionResult.VALID, data.releaseUse());
        assertEquals(PlayerActionData.TransitionResult.INVALID, data.releaseUse());
        data.reset();
        assertEquals(PlayerActionData.TransitionResult.UNKNOWN, data.releaseUse());
    }

    @Test
    public void ignoresUnknownDiggingTargetsAndFirstUseReleaseAfterReset()
    {
        final PlayerActionData data = new PlayerActionData();

        assertEquals(PlayerActionData.TransitionResult.UNKNOWN, data.finishDigging(1, 64, -2));
        assertEquals(PlayerActionData.TransitionResult.UNKNOWN, data.finishDigging(1, 64, -2));

        data.reset();
        assertEquals(PlayerActionData.TransitionResult.UNKNOWN, data.releaseUse());
        assertEquals(PlayerActionData.TransitionResult.INVALID, data.releaseUse());
    }

    @Test
    public void mismatchedDiggingTargetIsInvalid()
    {
        final PlayerActionData data = new PlayerActionData();
        data.startDigging(1, 64, -2);

        assertEquals(PlayerActionData.TransitionResult.INVALID, data.finishDigging(2, 64, -2));
    }

    @Test
    public void staleDiggingCancellationDoesNotCreateAnInvalidTransition()
    {
        final PlayerActionData data = new PlayerActionData();
        data.startDigging(1, 64, -2);

        data.cancelDigging();

        assertEquals(PlayerActionData.TransitionResult.UNKNOWN, data.finishDigging(1, 64, -2));
        assertEquals(PlayerActionData.TransitionResult.UNKNOWN, data.finishDigging(1, 64, -2));
    }
}
