package de.photon.anticheataddition.protocol.packetwrappers.sentbyserver;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.anticheataddition.protocol.packetwrappers.AbstractPacket;

public class WrapperPlayServerRelEntityMove extends AbstractPacket implements IWrapperPlayServerRelEntityMove
{
    public static final PacketType TYPE = PacketType.Play.Server.REL_ENTITY_MOVE;

    public WrapperPlayServerRelEntityMove()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayServerRelEntityMove(PacketContainer packet)
    {
        super(packet, TYPE);
    }

    protected WrapperPlayServerRelEntityMove(PacketContainer packet, PacketType packetType)
    {
        super(packet, packetType);
    }
}
