package de.photon.aacadditionpro.protocol.packetwrappers.sentbyclient;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.aacadditionpro.protocol.packetwrappers.AbstractPacket;
import de.photon.aacadditionpro.protocol.packetwrappers.IWrapperPlayCustomPayload;

public class WrapperPlayClientCustomPayload extends AbstractPacket implements IWrapperPlayCustomPayload
{
    public static final PacketType TYPE = PacketType.Play.Client.CUSTOM_PAYLOAD;

    public WrapperPlayClientCustomPayload()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayClientCustomPayload(PacketContainer packet)
    {
        super(packet, TYPE);
    }
}