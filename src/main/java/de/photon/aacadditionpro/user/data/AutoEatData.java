package de.photon.aacadditionpro.user.data;

import de.photon.aacadditionpro.user.TimeData;
import de.photon.aacadditionpro.user.User;

public class AutoEatData extends TimeData
{
    public AutoEatData(User user)
    {
        // [0] = Last interact
        // [1] = timeout
        super(user, 0, 0);
    }
}
