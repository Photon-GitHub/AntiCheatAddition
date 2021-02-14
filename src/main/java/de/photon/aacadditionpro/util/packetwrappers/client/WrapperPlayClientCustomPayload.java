package de.photon.aacadditionpro.util.packetwrappers.client;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.aacadditionpro.util.packetwrappers.AbstractPacket;
import de.photon.aacadditionpro.util.packetwrappers.IWrapperPlayCustomPayload;

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