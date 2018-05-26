package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.user.Data;
import de.photon.AACAdditionPro.user.User;
import lombok.Getter;

import java.util.LinkedList;
import java.util.Objects;

public class KeepAliveData extends Data
{
    @Getter
    private final LinkedList<KeepAlivePacketData> keepAlives = new LinkedList<>();

    public KeepAliveData(User user)
    {
        super(user);
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
        private final int timeHash;
        @Getter
        private long timeDifference = -1;

        public KeepAlivePacketData(int timeHash) {this.timeHash = timeHash;}

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
            return timeHash == that.timeHash;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(timeHash);
        }
    }
}
