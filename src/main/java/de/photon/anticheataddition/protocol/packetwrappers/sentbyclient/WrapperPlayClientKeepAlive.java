package de.photon.anticheataddition.protocol.packetwrappers.sentbyclient;


import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.anticheataddition.protocol.packetwrappers.WrapperPlayKeepAlive;

public class WrapperPlayClientKeepAlive extends WrapperPlayKeepAlive
{
    public static final PacketType TYPE = PacketType.Play.Client.KEEP_ALIVE;

    public WrapperPlayClientKeepAlive()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayClientKeepAlive(PacketContainer packet)
    {
        super(packet, TYPE);
    }
}