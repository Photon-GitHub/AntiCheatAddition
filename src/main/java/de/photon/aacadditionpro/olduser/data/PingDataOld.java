package de.photon.aacadditionpro.olduser.data;

import de.photon.aacadditionpro.olduser.TimeDataOld;
import de.photon.aacadditionpro.olduser.UserOld;
import org.bukkit.Location;

public class PingDataOld extends TimeDataOld
{
    public boolean isCurrentlyChecking;
    public boolean forceUpdatePing;
    public Location teleportLocation;

    public PingDataOld(final UserOld user)
    {
        super(user, 0);
    }
}
