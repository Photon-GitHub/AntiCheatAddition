package de.photon.aacadditionproold.util.packetwrappers.server;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.aacadditionproold.util.packetwrappers.AbstractPacket;
import de.photon.aacadditionproold.util.packetwrappers.IWrapperPlayEntity;

public class WrapperPlayServerEntity extends AbstractPacket implements IWrapperPlayEntity
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
