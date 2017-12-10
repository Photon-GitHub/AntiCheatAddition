package de.photon.AACAdditionPro.util.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

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