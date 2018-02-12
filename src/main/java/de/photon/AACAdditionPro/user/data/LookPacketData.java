package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;

public class LookPacketData extends TimeData
{
    // EqualRotation
    public float lastYaw;
    public float lastPitch;

    public LookPacketData(final User user)
    {
        super(user, 0);
    }
}
