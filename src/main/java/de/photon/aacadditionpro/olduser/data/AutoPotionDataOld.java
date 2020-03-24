package de.photon.aacadditionpro.olduser.data;

import de.photon.aacadditionpro.olduser.TimeDataOld;
import de.photon.aacadditionpro.olduser.UserOld;

public class AutoPotionDataOld extends TimeDataOld
{
    // AutoPotion
    public float lastSuddenYaw;
    public float lastSuddenPitch;
    public boolean alreadyThrown;

    public AutoPotionDataOld(final UserOld user)
    {
        // First Timestamp: Used for detection, second one for timeout
        super(user, 0, 0);
    }
}
