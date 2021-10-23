package de.photon.aacadditionpro.protocol.packetwrappers.sentbyclient;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.aacadditionpro.protocol.packetwrappers.AbstractPacket;

public class WrapperPlayClientLook extends AbstractPacket implements IWrapperPlayClientLook
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
}
