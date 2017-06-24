package de.photon.AACAdditionPro.userdata.data;

import de.photon.AACAdditionPro.userdata.User;
import org.bukkit.Location;

public class PingData extends TimeData
{
    public PingData(final User theUser)
    {
        super(false, theUser);
    }

    public boolean isCurrentlyChecking;
    public Location teleportLocation;
}
