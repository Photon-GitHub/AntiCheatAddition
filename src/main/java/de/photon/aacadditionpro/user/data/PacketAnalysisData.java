package de.photon.aacadditionpro.user.data;

import de.photon.aacadditionpro.user.TimeData;
import de.photon.aacadditionpro.user.User;
import lombok.Getter;
import org.bukkit.Location;

public class PacketAnalysisData extends TimeData
{
    public PositionForceData lastPositionForceData = null;
    public long compareFails = 0;

    public boolean animationExpected = false;

    // After cancelling a move packet one equalRotation is expected.
    public boolean equalRotationExpected = false;

    public PacketAnalysisData(User user)
    {
        // [0] = The last compare flag
        super(user, 0);
    }

    @Override
    public void unregister()
    {
        super.unregister();
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
}
