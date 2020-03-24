package de.photon.aacadditionpro.olduser.data;

import de.photon.aacadditionpro.olduser.TimeDataOld;
import de.photon.aacadditionpro.olduser.UserOld;

public class AutoEatDataOld extends TimeDataOld
{
    public AutoEatDataOld(UserOld user)
    {
        // [0] = Last interact
        // [1] = timeout
        super(user, 0, 0);
    }
}
