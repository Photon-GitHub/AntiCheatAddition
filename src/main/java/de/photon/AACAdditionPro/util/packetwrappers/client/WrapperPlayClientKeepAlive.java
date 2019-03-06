package de.photon.AACAdditionPro.util.packetwrappers.client;


import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayKeepAlive;

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