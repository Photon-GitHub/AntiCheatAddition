package de.photon.aacadditionproold.util.packetwrappers;

public interface IWrapperPlayLook extends IWrapperPlayOnGround
{
    /**
     * Retrieve Yaw.
     * <p>
     * Notes: absolute rotation on the X Axis, in degrees
     *
     * @return The current Yaw
     */
    float getYaw();

    /**
     * Set Yaw.
     *
     * @param value - new value.
     */
    void setYaw(final float value);

    /**
     * Retrieve Pitch.
     * <p>
     * Notes: absolute rotation on the Y Axis, in degrees
     *
     * @return The current Pitch
     */
    float getPitch();

    /**
     * Set Pitch.
     *
     * @param value - new value.
     */
    void setPitch(final float value);
}
