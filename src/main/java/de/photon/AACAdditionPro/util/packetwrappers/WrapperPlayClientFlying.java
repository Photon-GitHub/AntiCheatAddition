package de.photon.AACAdditionPro.util.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class WrapperPlayClientFlying extends AbstractPacket
{
    public static final PacketType TYPE = PacketType.Play.Client.FLYING;

    public WrapperPlayClientFlying()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayClientFlying(final PacketContainer packet)
    {
        super(packet, TYPE);
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