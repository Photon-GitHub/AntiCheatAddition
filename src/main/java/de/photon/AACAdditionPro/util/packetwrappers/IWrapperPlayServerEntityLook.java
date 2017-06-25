package de.photon.AACAdditionPro.util.packetwrappers;

import com.comphenix.protocol.events.PacketContainer;

public interface IWrapperPlayServerEntityLook extends IWrapperPlayServerEntityOnGround
{

    /**
     * Retrieve the yaw of the current entity.
     *
     * @return The current Yaw
     */
    default float getYaw()
    {
        return (getHandle().getBytes().read(0) * 360.F) / 256.0F;
    }

    /**
     * Set the yaw of the current entity.
     *
     * @param value - new yaw.
     */
    default void setYaw(float value)
    {
        getHandle().getBytes().write(0, (byte) (value * 256.0F / 360.0F));
    }

    /**
     * Retrieve the pitch of the current entity.
     *
     * @return The current pitch
     */
    default float getPitch()
    {
        return (getHandle().getBytes().read(1) * 360.F) / 256.0F;
    }

    /**
     * Set the pitch of the current entity.
     *
     * @param value - new pitch.
     */
    default void setPitch(float value)
    {
        getHandle().getBytes().write(1, (byte) (value * 256.0F / 360.0F));
    }

}
