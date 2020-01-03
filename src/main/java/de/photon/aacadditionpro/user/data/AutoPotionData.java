package de.photon.aacadditionpro.user.data;

import de.photon.aacadditionpro.user.TimeData;
import de.photon.aacadditionpro.user.User;

public class AutoPotionData extends TimeData
{
    // AutoPotion
    public float lastSuddenYaw;
    public float lastSuddenPitch;
    public boolean alreadyThrown;

    public AutoPotionData(final User user)
    {
        // First Timestamp: Used for detection, second one for timeout
        super(user, 0, 0);
    }
}
