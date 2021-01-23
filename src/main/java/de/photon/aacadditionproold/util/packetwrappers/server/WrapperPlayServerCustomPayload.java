package de.photon.aacadditionproold.util.packetwrappers.server;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.aacadditionproold.util.packetwrappers.AbstractPacket;
import de.photon.aacadditionproold.util.packetwrappers.IWrapperPlayCustomPayload;

public class WrapperPlayServerCustomPayload extends AbstractPacket implements IWrapperPlayCustomPayload
{
    public static final PacketType TYPE = PacketType.Play.Server.CUSTOM_PAYLOAD;

    public WrapperPlayServerCustomPayload()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayServerCustomPayload(final PacketContainer packet)
    {
        super(packet, TYPE);
    }
}