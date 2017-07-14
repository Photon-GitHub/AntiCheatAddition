package de.photon.AACAdditionPro.util.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class WrapperPlayServerEntity extends AbstractPacket implements IWrapperPlayServerEntity
{
    public static final PacketType TYPE = PacketType.Play.Server.ENTITY;

    public WrapperPlayServerEntity()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayServerEntity(PacketContainer packet)
    {
        super(packet, TYPE);
    }

    protected WrapperPlayServerEntity(PacketContainer packet, PacketType packetType)
    {
        super(packet, packetType);
    }
}
