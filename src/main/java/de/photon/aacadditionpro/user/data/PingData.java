package de.photon.aacadditionpro.user.data;

import de.photon.aacadditionpro.user.TimeData;
import de.photon.aacadditionpro.user.User;
import org.bukkit.Location;

public class PingData extends TimeData
{
    public boolean isCurrentlyChecking;
    public boolean forceUpdatePing;
    public Location teleportLocation;

    public PingData(final User user)
    {
        super(user, 0);
    }
}
