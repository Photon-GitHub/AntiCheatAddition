package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;

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
