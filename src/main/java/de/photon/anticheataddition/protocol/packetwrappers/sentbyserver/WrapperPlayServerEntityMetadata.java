package de.photon.anticheataddition.protocol.packetwrappers.sentbyserver;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import de.photon.anticheataddition.protocol.packetwrappers.IWrapperPlayEntity;
import de.photon.anticheataddition.protocol.packetwrappers.MetadataPacket;

import java.util.List;

public class WrapperPlayServerEntityMetadata extends MetadataPacket implements IWrapperPlayEntity
{
    public static final PacketType TYPE = PacketType.Play.Server.ENTITY_METADATA;

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
     * THIS IS ONLY VALID ON 1.19.3+
     *
     * @return The current Metadata
     */
    @Override
    public List<WrappedDataValue> getMetadata()
    {
        return handle.getDataValueCollectionModifier().read(0);
    }

    @Override
    public List<WrappedWatchableObject> getLegacyMetadata()
    {
        return handle.getWatchableCollectionModifier().read(0);
    }


    /**
     * Set Metadata.
     * THIS IS ONLY VALID ON 1.19.3+
     *
     * @param value - new value.
     */
    public void setMetadata(List<WrappedDataValue> value)
    {
        handle.getDataValueCollectionModifier().write(0, value);
    }

    public void setLegacyMetadata(List<WrappedWatchableObject> value)
    {
        handle.getWatchableCollectionModifier().write(0, value);
    }
}