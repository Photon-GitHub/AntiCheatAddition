package de.photon.aacadditionpro.protocol.packetwrappers.sentbyserver;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.aacadditionpro.protocol.packetwrappers.WrapperPlayKeepAlive;

public class WrapperPlayServerKeepAlive extends WrapperPlayKeepAlive
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