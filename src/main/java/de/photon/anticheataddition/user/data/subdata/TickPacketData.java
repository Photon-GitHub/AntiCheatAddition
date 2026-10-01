package de.photon.anticheataddition.user.data.subdata;

import de.photon.anticheataddition.user.data.Timestamp;
import de.photon.anticheataddition.user.data.ViolationCounter;

import java.util.concurrent.TimeUnit;

/**
 * Counts a tick-driven packet stream, including the first partial tick. Client markers delimit packets but cannot
 * clear failure evidence or the report cooldown. Only a quiet interval measured by the server permits recovery.
 */
public final class TickPacketData
{
    private static final long RECOVERY_NANOS = TimeUnit.SECONDS.toNanos(10);
    private static final long REPORT_COOLDOWN_NANOS = TimeUnit.SECONDS.toNanos(1);

    private final ViolationCounter duplicatePackets = new ViolationCounter(3);
    private boolean packetThisTick;
    private long lastExcessTimestamp;
    private long lastReportedTimestamp;

    public synchronized boolean recordPacket(long nanos_timestamp)
    {
        if (!packetThisTick) {
            packetThisTick = true;
            return false;
        }

        if (lastExcessTimestamp != 0L && nanos_timestamp - lastExcessTimestamp >= RECOVERY_NANOS) duplicatePackets.setToZero();
        lastExcessTimestamp = nanos_timestamp;

        if (duplicatePackets.smallerThanThreshold()) duplicatePackets.increment();
        if (duplicatePackets.smallerThanThreshold()) return false;
        if (lastReportedTimestamp != 0L && nanos_timestamp - lastReportedTimestamp < REPORT_COOLDOWN_NANOS) return false;

        lastReportedTimestamp = nanos_timestamp;
        return true;
    }

    public synchronized void endTick()
    {
        packetThisTick = false;
    }

    public synchronized void reset()
    {
        packetThisTick = false;
        duplicatePackets.setToZero();
        lastExcessTimestamp = 0L;
        lastReportedTimestamp = 0L;
    }
}
