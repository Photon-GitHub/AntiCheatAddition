package de.photon.AACAdditionPro.util.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class WrapperPlayClientLook extends AbstractPacket
{
    public static final PacketType TYPE = PacketType.Play.Client.LOOK;

    public WrapperPlayClientLook()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayClientLook(final PacketContainer packet)
    {
        super(packet, TYPE);
    }

    /**
     * Retrieve Yaw.
     * <p>
     * Notes: absolute rotation on the X Axis, in degrees
     *
     * @return The current Yaw
     */
    public float getYaw()
    {
        return handle.getFloat().read(0);
    }

    /**
     * Set Yaw.
     *
     * @param value - new value.
     */
    public void setYaw(final float value)
    {
        handle.getFloat().write(0, value);
    }

    /**
     * Retrieve Pitch.
     * <p>
     * Notes: absolute rotation on the Y Axis, in degrees
     *
     * @return The current Pitch
     */
    public float getPitch()
    {
        return handle.getFloat().read(1);
    }

    /**
     * Set Pitch.
     *
     * @param value - new value.
     */
    public void setPitch(final float value)
    {
        handle.getFloat().write(1, value);
    }

    /**
     * Retrieve On Ground.
     * <p>
     * Notes: true if the client is on the ground, False otherwise
     *
     * @return The current On Ground
     */
    public boolean getOnGround()
    {
        return handle.getBooleans().read(0);
    }

    /**
     * Set On Ground.
     *
     * @param value - new value.
     */
    public void setOnGround(final boolean value)
    {
        handle.getBooleans().write(0, value);
    }

}