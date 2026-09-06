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
        assertTrue(data.observeSequence(10, true));
        assertTrue(data.observeSequence(17, true));
        assertFalse(data.observeSequence(17, true));
        assertFalse(data.observeSequence(16, true));
    }

    @Test
    public void ignoresSequenceValidationOnOldProtocols()
    {
        final PlayerActionData data = new PlayerActionData();
        assertTrue(data.observeSequence(-1, false));
        assertTrue(data.observeSequence(-1, false));
    }

    @Test
    public void tracksDiggingTargetAndUseState()
    {
        final PlayerActionData data = new PlayerActionData();
        data.startDigging(1, 64, -2, 1);
        assertEquals(PlayerActionData.TransitionResult.VALID, data.finishDigging(1, 64, -2, 1));
        assertEquals(PlayerActionData.TransitionResult.INVALID, data.finishDigging(1, 64, -2, 1));

        data.startUse();
        assertEquals(PlayerActionData.TransitionResult.VALID, data.releaseUse());
        assertEquals(PlayerActionData.TransitionResult.INVALID, data.releaseUse());
        data.reset();
        assertEquals(PlayerActionData.TransitionResult.UNKNOWN, data.releaseUse());
    }

    @Test
    public void ignoresFirstTerminalTransitionAfterLifecycleReset()
    {
        final PlayerActionData data = new PlayerActionData();

        assertEquals(PlayerActionData.TransitionResult.UNKNOWN, data.finishDigging(1, 64, -2, 1));
        assertEquals(PlayerActionData.TransitionResult.INVALID, data.finishDigging(1, 64, -2, 1));

        data.reset();
        assertEquals(PlayerActionData.TransitionResult.UNKNOWN, data.releaseUse());
        assertEquals(PlayerActionData.TransitionResult.INVALID, data.releaseUse());
    }

    @Test
    public void mismatchedDiggingTargetIsInvalid()
    {
        final PlayerActionData data = new PlayerActionData();
        data.startDigging(1, 64, -2, 1);

        assertEquals(PlayerActionData.TransitionResult.INVALID, data.finishDigging(2, 64, -2, 1));
    }

    @Test
    public void staleDiggingCancellationDoesNotCreateAnInvalidTransition()
    {
        final PlayerActionData data = new PlayerActionData();
        data.startDigging(1, 64, -2, 1);

        data.cancelDigging();

        assertEquals(PlayerActionData.TransitionResult.UNKNOWN, data.finishDigging(1, 64, -2, 1));
    }
}
