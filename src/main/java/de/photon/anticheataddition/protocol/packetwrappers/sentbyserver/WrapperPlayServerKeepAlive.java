package de.photon.anticheataddition.protocol.packetwrappers.sentbyserver;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.anticheataddition.protocol.packetwrappers.AbstractPacket;
import de.photon.anticheataddition.protocol.packetwrappers.IWrapperPlayKeepAlive;

public class WrapperPlayServerKeepAlive extends AbstractPacket implements IWrapperPlayKeepAlive
{
    public static final PacketType TYPE = PacketType.Play.Server.KEEP_ALIVE;

    public WrapperPlayServerKeepAlive()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayServerKeepAlive(PacketContainer packet)
    {
        super(packet, TYPE);
    }
}