package de.photon.AACAdditionPro.util.packetwrappers;


import de.photon.AACAdditionPro.util.mathematics.RotationUtil;

public interface IWrapperPlayServerEntityLook extends IWrapperPlayServerEntityOnGround
{
    /**
     * This is a method to support multiple packets in one interface.
     * Minecraft 1.8.8 has various other packet layouts that make this method one necessary.
     */
    default int getByteOffset()
    {
        return 0;
    }

    /**
     * Retrieve the yaw of the current entity.
     *
     * @return The current Yaw
     */
    default float getYaw()
    {
        return RotationUtil.convertFixedRotation(getHandle().getBytes().read(getByteOffset()));
    }

    /**
     * Set the yaw of the current entity.
     *
     * @param value - new yaw.
     */
    default void setYaw(float value)
    {
        getHandle().getBytes().write(getByteOffset(), RotationUtil.getFixRotation(value));
    }

    /**
     * Retrieve the pitch of the current entity.
     *
     * @return The current pitch
     */
    default float getPitch()
    {
        return RotationUtil.convertFixedRotation(getHandle().getBytes().read(1 + getByteOffset()));
    }

    /**
     * Set the pitch of the current entity.
     *
     * @param value - new pitch.
     */
    default void setPitch(float value)
    {
        getHandle().getBytes().write(1 + getByteOffset(), RotationUtil.getFixRotation(value));
    }
}
