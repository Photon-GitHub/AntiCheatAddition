package de.photon.anticheataddition.user.data.subdata;

public final class PlayerActionData
{
    private boolean hasSequence;
    private int lastSequence;
    private DigTarget diggingTarget;
    private boolean diggingStateKnown;
    private boolean activeUse;
    private boolean useStateKnown;

    public synchronized boolean observeSequence(final int sequence, final boolean sequenceSupported)
    {
        if (!sequenceSupported) return true;
        if (sequence < 0) return false;

        final boolean replay = hasSequence && sequence <= lastSequence;
        if (!replay) {
            hasSequence = true;
            lastSequence = sequence;
        }
        return !replay;
    }

    public synchronized void startDigging(final int x, final int y, final int z, final int face)
    {
        diggingTarget = new DigTarget(x, y, z, face);
        diggingStateKnown = true;
    }

    public synchronized TransitionResult finishDigging(final int x, final int y, final int z, final int face)
    {
        if (diggingTarget == null) {
            final TransitionResult result = diggingStateKnown ? TransitionResult.INVALID : TransitionResult.UNKNOWN;
            diggingStateKnown = true;
            return result;
        }
        final boolean matches = diggingTarget.x() == x &&
                                diggingTarget.y() == y &&
                                diggingTarget.z() == z &&
                                diggingTarget.face() == face;
        diggingTarget = null;
        diggingStateKnown = true;
        return matches ? TransitionResult.VALID : TransitionResult.INVALID;
    }

    public synchronized void clearDigging()
    {
        diggingTarget = null;
        diggingStateKnown = true;
    }

    public synchronized void cancelDigging()
    {
        diggingTarget = null;
        // The cancellation may refer to a stale client-side target. Do not let a later
        // completion be judged against the target that was just invalidated.
        diggingStateKnown = false;
    }

    public synchronized void startUse()
    {
        activeUse = true;
        useStateKnown = true;
    }

    public synchronized void clearUse()
    {
        activeUse = false;
        useStateKnown = true;
    }

    public synchronized TransitionResult releaseUse()
    {
        if (activeUse) {
            activeUse = false;
            useStateKnown = true;
            return TransitionResult.VALID;
        }
        final TransitionResult result = useStateKnown ? TransitionResult.INVALID : TransitionResult.UNKNOWN;
        useStateKnown = true;
        return result;
    }

    public synchronized void reset()
    {
        hasSequence = false;
        lastSequence = 0;
        diggingTarget = null;
        diggingStateKnown = false;
        activeUse = false;
        useStateKnown = false;
    }

    private record DigTarget(int x, int y, int z, int face) {}

    public enum TransitionResult
    {
        VALID,
        INVALID,
        UNKNOWN
    }
}
