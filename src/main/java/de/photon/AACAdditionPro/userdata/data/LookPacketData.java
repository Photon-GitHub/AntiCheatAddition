package de.photon.AACAdditionPro.userdata.data;

import de.photon.AACAdditionPro.userdata.User;

public class LookPacketData extends TimeData
{
    // EqualRotation
    public float lastYaw;
    public float lastPitch;

    public LookPacketData(final User theUser)
    {
        super(false, theUser);
    }
}
