package de.photon.aacadditionpro.olduser.data;

import de.photon.aacadditionpro.olduser.TimeDataOld;
import de.photon.aacadditionpro.olduser.UserOld;
import lombok.Getter;
import org.bukkit.Location;

public class PacketAnalysisDataOld extends TimeDataOld
{
    public PositionForceData lastPositionForceData = null;
    public long compareFails = 0;

    public boolean animationExpected = false;

    // After cancelling a move packet one equalRotation is expected.
    public boolean equalRotationExpected = false;

    public PacketAnalysisDataOld(UserOld user)
    {
        // [0] = The last compare flag
        super(user, 0);
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
