package de.photon.aacadditionpro.util.packetwrappers.server;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import de.photon.aacadditionpro.util.packetwrappers.IWrapperPlayEntity;
import de.photon.aacadditionpro.util.packetwrappers.MetadataPacket;

import java.util.List;

public class WrapperPlayServerEntityMetadata extends MetadataPacket implements IWrapperPlayEntity
{
    public static final PacketType TYPE =
            PacketType.Play.Server.ENTITY_METADATA;

    public WrapperPlayServerEntityMetadata()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayServerEntityMetadata(PacketContainer packet)
    {
        super(packet, TYPE);
    }

    /**
     * Retrieve Metadata.
     *
     * @return The current Metadata
     */
    public List<WrappedWatchableObject> getMetadata()
    {
        return handle.getWatchableCollectionModifier().read(0);
    }

    /**
     * Set Metadata.
     *
     * @param value - new value.
     */
    public void setMetadata(List<WrappedWatchableObject> value)
    {
        handle.getWatchableCollectionModifier().write(0, value);
    }
}