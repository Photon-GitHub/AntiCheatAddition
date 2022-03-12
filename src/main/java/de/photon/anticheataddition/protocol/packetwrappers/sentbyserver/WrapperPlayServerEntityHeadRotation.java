package de.photon.anticheataddition.protocol.packetwrappers.sentbyserver;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.anticheataddition.protocol.packetwrappers.AbstractPacket;
import de.photon.anticheataddition.protocol.packetwrappers.IWrapperPlayEntity;

public class WrapperPlayServerEntityHeadRotation extends AbstractPacket implements IWrapperPlayEntity
{
    public static final PacketType TYPE =
            PacketType.Play.Server.ENTITY_HEAD_ROTATION;

    public WrapperPlayServerEntityHeadRotation()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayServerEntityHeadRotation(PacketContainer packet)
    {
        super(packet, TYPE);
    }

    /**
     * Retrieve Head Yaw.
     * <p>
     * Notes: head yaw in steps of 2p/256
     *
     * @return The current Head Yaw
     */
    public byte getHeadYaw()
    {
        return handle.getBytes().read(0);
    }

    /**
     * Set Head Yaw.
     *
     * @param value - new value.
     */
    public void setHeadYaw(byte value)
    {
        handle.getBytes().write(0, value);
    }
}