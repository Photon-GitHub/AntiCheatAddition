package de.photon.anticheataddition.user.data.subdata;

public final class PacketFloodData
{
    private static final long RECOVERY_NANOS = 1_000_000_000L;

    private double tokens;
    private long lastTimestamp;
    private long exhaustedSince;
    private long lastExcessTimestamp;
    private long lastReportedTimestamp;

    public synchronized Result record(final long currentTimestamp, final double refillPerSecond, final double capacity, final long sustainedDurationNanos)
    {
        if (refillPerSecond <= 0D || capacity <= 0D || sustainedDurationNanos < 0L) {
            throw new IllegalArgumentException("Packet rate and capacity must be positive and duration non-negative.");
        }

        if (lastTimestamp == 0L || currentTimestamp < lastTimestamp) {
            tokens = capacity;
            exhaustedSince = 0L;
            lastExcessTimestamp = 0L;
        } else {
            final long elapsed = currentTimestamp - lastTimestamp;
            tokens = Math.min(capacity, tokens + elapsed / 1_000_000_000D * refillPerSecond);
        }
        lastTimestamp = currentTimestamp;

        if (tokens < 1D) {
            if (lastExcessTimestamp != 0L && currentTimestamp - lastExcessTimestamp > RECOVERY_NANOS) {
                exhaustedSince = currentTimestamp;
            }
            if (exhaustedSince == 0L) exhaustedSince = currentTimestamp;
            lastExcessTimestamp = currentTimestamp;
            return new Result(true, currentTimestamp - exhaustedSince >= sustainedDurationNanos);
        }

        tokens -= 1D;
        final boolean throttled = isThrottled(currentTimestamp);
        return new Result(throttled, false);
    }

    /**
     * Returns whether excess movement packets are still arriving. A short recovery timeout prevents one attacker
     * controlled burst from suppressing targeting analysis for the remainder of the session.
     */
    public synchronized boolean isThrottled(final long currentTimestamp)
    {
        if (exhaustedSince == 0L) return false;
        if (currentTimestamp >= lastExcessTimestamp && currentTimestamp - lastExcessTimestamp > RECOVERY_NANOS) {
            exhaustedSince = 0L;
            lastExcessTimestamp = 0L;
            return false;
        }
        return true;
    }

    public synchronized boolean shouldReport(final long currentTimestamp, final long cooldownNanos)
    {
        if (lastReportedTimestamp != 0L && currentTimestamp - lastReportedTimestamp < cooldownNanos) return false;
        lastReportedTimestamp = currentTimestamp;
        return true;
    }

    public synchronized void reset()
    {
        tokens = 0D;
        lastTimestamp = 0L;
        exhaustedSince = 0L;
        lastExcessTimestamp = 0L;
        lastReportedTimestamp = 0L;
    }

    public record Result(boolean throttled, boolean sustained) {}
}
