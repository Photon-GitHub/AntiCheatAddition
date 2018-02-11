package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;

public class VelocityChangeData extends TimeData
{
    public boolean positiveVelocity;
    public int velocityChangeCounter;

    public VelocityChangeData(final User user)
    {
        super(user, 0);
    }
}
