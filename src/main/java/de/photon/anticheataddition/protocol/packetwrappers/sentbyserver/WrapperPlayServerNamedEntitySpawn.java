package de.photon.anticheataddition.protocol.packetwrappers.sentbyserver;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import de.photon.anticheataddition.ServerVersion;
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
        return ServerVersion.is18() ?
               handle.getIntegers().read(1) / 32.0D :
               handle.getDoubles().read(0);
    }

    @Override
    public void setX(double value)
    {
        if (ServerVersion.is18()) handle.getIntegers().write(1, (int) (value * 32));
        else handle.getDoubles().write(0, value);
    }

    @Override
    public double getY()
    {
        return ServerVersion.is18() ?
               handle.getIntegers().read(2) / 32.0D :
               handle.getDoubles().read(1);
    }

    @Override
    public void setY(double value)
    {
        if (ServerVersion.is18()) handle.getIntegers().write(2, (int) (value * 32));
        else handle.getDoubles().write(1, value);
    }

    @Override
    public double getZ()
    {
        return ServerVersion.is18() ?
               handle.getIntegers().read(3) / 32.0D :
               handle.getDoubles().read(2);
    }

    @Override
    public void setZ(double value)
    {
        if (ServerVersion.is18()) handle.getIntegers().write(3, (int) (value * 32));
        else handle.getDoubles().write(2, value);
    }

    /**
     * Retrieve Metadata.
     * <p>
     * Notes: the client will crash if no metadata is sent
     *
     * @return The current Metadata
     */
    public WrappedDataWatcher getMetadata()
    {
        return handle.getDataWatcherModifier().read(0);
    }

    /**
     * Set Metadata.
     *
     * @param value - new value.
     */
    public void setMetadata(WrappedDataWatcher value)
    {
        handle.getDataWatcherModifier().write(0, value);
    }

    @Override
    public List<WrappedWatchableObject> getRawMetadata()
    {
        return getMetadata().getWatchableObjects();
    }
}
