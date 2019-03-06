package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;
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
