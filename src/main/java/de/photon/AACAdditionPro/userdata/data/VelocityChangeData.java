package de.photon.AACAdditionPro.userdata.data;

import de.photon.AACAdditionPro.userdata.User;

public class VelocityChangeData extends TimeData
{
    public boolean positiveVelocity;
    public int velocityChangeCounter;

    public VelocityChangeData(User theUser)
    {
        super(false, theUser);
    }
}
