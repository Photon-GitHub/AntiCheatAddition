package de.photon.AACAdditionPro.userdata.data;

import de.photon.AACAdditionPro.userdata.User;

public class AutoPotionData extends TimeData
{
    // AutoPotion
    public float lastSuddenYaw;
    public float lastSuddenPitch;
    public boolean alreadyThrown;

    // First Timestamp: Used for detection, second one for timeout
    public AutoPotionData(final User theUser)
    {
        super(false, theUser, 0, 0);
    }
}
