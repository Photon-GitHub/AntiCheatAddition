package de.photon.anticheataddition.protocol.packetwrappers.sentbyserver;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import de.photon.anticheataddition.protocol.packetwrappers.IWrapperPlayEntity;
import de.photon.anticheataddition.protocol.packetwrappers.IWrapperPlayPosition;
import de.photon.anticheataddition.protocol.packetwrappers.MetadataPacket;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.UUID;

public class WrapperPlayServerNamedEntitySpawn extends MetadataPacket implements IWrapperPlayEntity, IWrapperPlayPosition, IWrapperPlayServerLook
{
    public static final PacketType TYPE = PacketType.Play.Server.NAMED_ENTITY_SPAWN;

    public WrapperPlayServerNamedEntitySpawn()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayServerNamedEntitySpawn(PacketContainer packet)
    {
        super(packet, TYPE);
    }

    /**
     * Retrieve Player UUID.
     * <p>
     * Notes: player's UUID
     *
     * @return The current Player UUID
     */
    public UUID getPlayerUUID()
    {
        return handle.getUUIDs().read(0);
    }

    /**
     * Set Player UUID.
     *
     * @param value - new value.
     */
    public void setPlayerUUID(UUID value)
    {
        handle.getUUIDs().write(0, value);
    }

    /**
     * Set the position of the spawned entity using a vector.
     *
     * @param position - the new position.
     */
    public void setPosition(Vector position)
    {
        setX(position.getX());
        setY(position.getY());
        setZ(position.getZ());
    }

    @Override
    public double getX()
    {
        return IWrapperPlayPosition.getIntDoublePosition(handle, 0);
    }

    @Override
    public void setX(double value)
    {
        IWrapperPlayPosition.setIntDoublePosition(handle, 0, value);
    }

    @Override
    public double getY()
    {
        return IWrapperPlayPosition.getIntDoublePosition(handle, 1);
    }

    @Override
    public void setY(double value)
    {
        IWrapperPlayPosition.setIntDoublePosition(handle, 1, value);
    }

    @Override
    public double getZ()
    {
        return IWrapperPlayPosition.getIntDoublePosition(handle, 2);
    }

    @Override
    public void setZ(double value)
    {
        IWrapperPlayPosition.setIntDoublePosition(handle, 2, value);
    }

    /**
     * Retrieve Metadata.
     * <p>
     * Notes: the client will crash if no metadata is sent
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
}
