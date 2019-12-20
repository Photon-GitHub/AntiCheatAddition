package de.photon.aacadditionpro.util.packetwrappers.server;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.util.exceptions.UnknownMinecraftVersion;
import de.photon.aacadditionpro.util.packetwrappers.IWrapperPlayEntity;
import de.photon.aacadditionpro.util.packetwrappers.IWrapperPlayPosition;
import de.photon.aacadditionpro.util.packetwrappers.MetadataPacket;
import org.bukkit.util.Vector;

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
     * Retrieve the position of the spawned entity as a vector.
     *
     * @return The position as a vector.
     */
    public Vector getPosition()
    {
        return new Vector(getX(), getY(), getZ());
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
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                return handle.getIntegers().read(1) / 32.0D;
            case MC113:
            case MC114:
                return handle.getDoubles().read(0);
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    @Override
    public void setX(double value)
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                handle.getIntegers().write(1, (int) (value * 32));
                break;
            case MC113:
            case MC114:
            case MC115:
                handle.getDoubles().write(0, value);
                break;
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    @Override
    public double getY()
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                return handle.getIntegers().read(2) / 32.0D;
            case MC113:
            case MC114:
            case MC115:
                return handle.getDoubles().read(1);
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    @Override
    public void setY(double value)
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                handle.getIntegers().write(2, (int) (value * 32));
                break;
            case MC113:
            case MC114:
            case MC115:
                handle.getDoubles().write(1, value);
                break;
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    @Override
    public double getZ()
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                return handle.getIntegers().read(3) / 32.0D;
            case MC113:
            case MC114:
            case MC115:
                return handle.getDoubles().read(2);
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    @Override
    public void setZ(double value)
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                handle.getIntegers().write(3, (int) (value * 32));
                break;
            case MC113:
            case MC114:
            case MC115:
                handle.getDoubles().write(2, value);
                break;
            default:
                throw new UnknownMinecraftVersion();
        }
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
}
