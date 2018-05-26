package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.user.Data;
import de.photon.AACAdditionPro.user.User;
import lombok.Getter;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class KeepAliveData extends Data
{
    public static final byte KEEPALIVE_QUEUE_SIZE = 20;

    @Getter
    private final List<KeepAlivePacketData> keepAlives = Collections.synchronizedList(new LinkedList<>());

    public KeepAliveData(User user)
    {
        super(user);
    }

    /**
     * Calculates how long the client needs to answer a KeepAlive packet on average.
     * Uses the whole List for the calculation.
     */
    public long averageResponseTime()
    {
        long sum = 0;
        int size = keepAlives.size();

        for (final KeepAlivePacketData keepAlive : keepAlives)
        {
            // Leave out ignored packets.
            if (keepAlive.timeDifference < 0)
            {
                size--;
                continue;
            }

            sum += keepAlive.timeDifference;
        }
        return sum / size;
    }

    /**
     * Calculates how long the client needs to answer a KeepAlive packet on average.
     * Only uses the last 3 values for the calculation.
     */
    public long recentResponseTime()
    {
        long sum = 0;
        int datapoints = 0;

        for (int i = keepAlives.size() - 1; i >= 0 && datapoints <= 3; i--)
        {
            // Leave out ignored packets.
            if (keepAlives.get(i).timeDifference < 0)
            {
                continue;
            }

            sum += keepAlives.get(i).timeDifference;
            datapoints++;
        }
        return sum / datapoints;
    }

    @Override
    public void unregister()
    {
        keepAlives.clear();
    }

    public static class KeepAlivePacketData
    {
        private final long timestamp = System.currentTimeMillis();
        @Getter
        private final int keepAliveID;
        @Getter
        private long timeDifference = -1;

        public KeepAlivePacketData(int keepAliveID) {this.keepAliveID = keepAliveID;}

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
