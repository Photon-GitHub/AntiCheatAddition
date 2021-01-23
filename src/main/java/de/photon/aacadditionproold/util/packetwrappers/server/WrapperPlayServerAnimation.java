package de.photon.aacadditionproold.util.packetwrappers.server;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.aacadditionproold.util.packetwrappers.AbstractPacket;
import de.photon.aacadditionproold.util.packetwrappers.IWrapperPlayEntity;

public class WrapperPlayServerAnimation extends AbstractPacket implements IWrapperPlayEntity
{
    public static final PacketType TYPE = PacketType.Play.Server.ANIMATION;

    public WrapperPlayServerAnimation()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayServerAnimation(PacketContainer packet)
    {
        super(packet, TYPE);
    }

    /**
     * Retrieve Animation.
     * <p>
     * Notes: animation ID
     *
     * @return The current Animation
     */
    public int getAnimation()
    {
        return handle.getIntegers().read(1);
    }

    /**
     * Set Animation.
     *
     * @param value - new value.
     */
    public void setAnimation(int value)
    {
        handle.getIntegers().write(1, value);
    }

}