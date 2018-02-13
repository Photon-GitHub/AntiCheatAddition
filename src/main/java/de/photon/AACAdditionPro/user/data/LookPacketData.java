package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.mathematics.RotationUtil;
import lombok.Getter;

public class LookPacketData extends TimeData
{
    // EqualRotation
    @Getter
    private float lastYaw;
    @Getter
    private float lastPitch;

    // First index is for timeout, second one for significant rotation changes (scaffold)
    public LookPacketData(final User user)
    {
        super(user, 0, 0);
    }

    public void updateRotations(final float yaw, final float pitch)
    {
        // Huge angle change
        if (RotationUtil.getDirection(lastYaw, lastPitch).angle(RotationUtil.getDirection(yaw, pitch)) > 35)
        {
            this.updateTimeStamp(1);
        }
    }
}
