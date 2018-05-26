package de.photon.AACAdditionPro.util.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class WrapperPlayClientKeepAlive extends AbstractPacket
{
    public static final PacketType TYPE = PacketType.Play.Client.KEEP_ALIVE;

    public WrapperPlayClientKeepAlive()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayClientKeepAlive(PacketContainer packet)
    {
        super(packet, TYPE);
    }

    /**
     * Retrieve Keep Alive ID.
     *
     * @return The current Keep Alive ID
     */
    public int getKeepAliveId()
    {
        return handle.getIntegers().read(0);
    }

    /**
     * Set Keep Alive ID.
     *
     * @param value - new value.
     */
    public void setKeepAliveId(int value)
    {
        handle.getIntegers().write(0, value);
    }
}