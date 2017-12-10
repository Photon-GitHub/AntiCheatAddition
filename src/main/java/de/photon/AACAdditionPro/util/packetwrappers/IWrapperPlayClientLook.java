package de.photon.AACAdditionPro.util.packetwrappers;

public interface IWrapperPlayClientLook extends IWrapperPlayClientOnGround
{
    /**
     * Retrieve Yaw.
     * <p>
     * Notes: absolute rotation on the X Axis, in degrees
     *
     * @return The current Yaw
     */
    default float getYaw()
    {
        return this.getHandle().getFloat().read(0);
    }

    /**
     * Set Yaw.
     *
     * @param value - new value.
     */
    default void setYaw(final float value)
    {
        this.getHandle().getFloat().write(0, value);
    }

    /**
     * Retrieve Pitch.
     * <p>
     * Notes: absolute rotation on the Y Axis, in degrees
     *
     * @return The current Pitch
     */
    default float getPitch()
    {
        return this.getHandle().getFloat().read(1);
    }

    /**
     * Set Pitch.
     *
     * @param value - new value.
     */
    default void setPitch(final float value)
    {
        this.getHandle().getFloat().write(1, value);
    }
}
