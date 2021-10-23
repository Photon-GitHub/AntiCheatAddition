package de.photon.aacadditionpro.protocol.packetwrappers.sentbyserver;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class WrapperPlayServerEntityLook extends WrapperPlayServerEntity implements IWrapperPlayServerLook
{
    public static final PacketType TYPE = PacketType.Play.Server.ENTITY_LOOK;

    public WrapperPlayServerEntityLook()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayServerEntityLook(PacketContainer packet)
    {
        super(packet, TYPE);
    }
}
