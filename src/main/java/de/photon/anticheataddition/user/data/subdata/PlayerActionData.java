package de.photon.anticheataddition.user.data.subdata;

public final class PlayerActionData
{
    private boolean hasSequence;
    private int lastSequence;
    private DigTarget diggingTarget;
    private boolean activeUse;
    private boolean useStateKnown;

    /**
     * Observes a block breaking packet sequence.
     *
     * @return true if the sequence is valid
     */
    public synchronized boolean observeSequence(final int sequence)
    {
        if (sequence < 0) return false;

        // Valid if there is no sequence yet or the sequence is correctly greater than the last one.
        if (!hasSequence || sequence > lastSequence) {
            hasSequence = true;
            lastSequence = sequence;
            return true;
        }
        return false;
    }

    public synchronized void startDigging(final int x, final int y, final int z)
    {
        diggingTarget = new DigTarget(x, y, z);
    }

    public synchronized TransitionResult finishDigging(final int x, final int y, final int z)
    {
        // Without an observed target, cancellation or an unobserved START can leave
        // several legitimate completions uncheckable until the next START arrives.
        if (diggingTarget == null) return TransitionResult.UNKNOWN;
        // The crosshair can move to another face of the same block while mining.
        final boolean matches = diggingTarget.x() == x &&
                                diggingTarget.y() == y &&
                                diggingTarget.z() == z;
        // Vanilla retains this position after completion. If placement restores the block
        // while attack remains held, the client can finish it again without another START.
        return matches ? TransitionResult.VALID : TransitionResult.INVALID;
    }

    public synchronized String describeDiggingTarget()
    {
        return diggingTarget == null ? "none" : diggingTarget.toString();
    }

    public synchronized void clearDigging()
    {
        diggingTarget = null;
    }

    public synchronized void cancelDigging()
    {
        diggingTarget = null;
        // The cancellation may refer to a stale client-side target. Do not let a later
        // completion be judged against the target that was just invalidated.
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
        activeUse = false;
        useStateKnown = false;
    }

    private record DigTarget(int x, int y, int z) {}

    public enum TransitionResult
    {
        VALID,
        INVALID,
        UNKNOWN
    }
}
