package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;
import lombok.Getter;
import org.bukkit.Location;

import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;

public class PacketAnalysisData extends TimeData
{
    // This needs to be so high to prevent flagging during TimeOuts.
    public static final byte KEEPALIVE_QUEUE_SIZE = 20;

    public PositionForceData lastPositionForceData = null;
    public long compareFails = 0;

    // Synchronized lists as the Protocol is async.
    @Getter
    private final Deque<KeepAlivePacketData> keepAlives = new ConcurrentLinkedDeque<>();

    public PacketAnalysisData(User user)
    {
        // [0] = The last compare flag
        super(user, 0);
    }

    /**
     * Calculates how long the client needs to answer a KeepAlive packet on average.
     * Only uses the last 4 values for the calculation.
     */
    public long recentKeepAliveResponseTime()
    {
        long sum = 0;
        int datapoints = 0;

        final Iterator<KeepAlivePacketData> iterator = keepAlives.descendingIterator();
        KeepAlivePacketData data;

        while (iterator.hasNext() && datapoints <= 3)
        {
            data = iterator.next();

            // Leave out ignored packets.
            if (data.timeDifference >= 0)
            {
                sum += data.timeDifference;
                datapoints++;
            }
        }
        return sum / datapoints;
    }

    @Override
    public void unregister()
    {
        keepAlives.clear();
    }

    public static class PositionForceData
    {
        private final long timestamp = System.currentTimeMillis();
        @Getter
        private final Location location;

        public PositionForceData(Location location) {this.location = location;}

        public long timeDifference()
        {
            return System.currentTimeMillis() - timestamp;
        }
    }

    public static class KeepAlivePacketData
    {
        private final long timestamp = System.currentTimeMillis();
        @Getter
        private final long keepAliveID;
        @Getter
        private long timeDifference = -1;

        public KeepAlivePacketData(long keepAliveID) {this.keepAliveID = keepAliveID;}

        public void registerResponse()
        {
            timeDifference = System.currentTimeMillis() - timestamp;
        }

        public boolean hasRegisteredResponse()
        {
            return timeDifference >= 0;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            KeepAlivePacketData that = (KeepAlivePacketData) o;
            return keepAliveID == that.keepAliveID;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(keepAliveID);
        }
    }
}
