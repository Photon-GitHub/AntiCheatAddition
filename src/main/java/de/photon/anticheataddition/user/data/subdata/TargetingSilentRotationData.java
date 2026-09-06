package de.photon.anticheataddition.user.data.subdata;

public final class TargetingSilentRotationData
{
    // Keep this as a primitive sliding window rather than the generic RingBuffer: it avoids boxed samples and tracks
    // the mismatch count incrementally instead of scanning the window after every use-item packet.
    private boolean[] mismatches = new boolean[0];
    private int writeIndex;
    private int size;
    private int mismatchCount;
    private long lastFlagTimestamp;

    public synchronized Window observe(final boolean mismatch,
                                       final int windowSize,
                                       final int requiredMismatches,
                                       final long currentTimestamp,
                                       final long flagCooldownNanos)
    {
        if (requiredMismatches <= 0 || requiredMismatches > windowSize) {
            throw new IllegalArgumentException("Silent rotation window limits are invalid.");
        }
        if (mismatches.length != windowSize) {
            mismatches = new boolean[windowSize];
            writeIndex = 0;
            size = 0;
            mismatchCount = 0;
        }

        if (size == windowSize && mismatches[writeIndex]) mismatchCount--;
        mismatches[writeIndex] = mismatch;
        if (mismatch) mismatchCount++;
        writeIndex = (writeIndex + 1) % windowSize;
        if (size < windowSize) size++;

        final boolean suspicious = size == windowSize && mismatchCount >= requiredMismatches;
        final boolean shouldFlag = suspicious &&
                                   (lastFlagTimestamp == 0L || currentTimestamp - lastFlagTimestamp >= flagCooldownNanos);
        if (shouldFlag) lastFlagTimestamp = currentTimestamp;
        return new Window(size, mismatchCount, suspicious, shouldFlag);
    }

    public synchronized void reset()
    {
        mismatches = new boolean[0];
        writeIndex = 0;
        size = 0;
        mismatchCount = 0;
        lastFlagTimestamp = 0L;
    }

    public record Window(int size, int mismatches, boolean suspicious, boolean shouldFlag) {}
}
