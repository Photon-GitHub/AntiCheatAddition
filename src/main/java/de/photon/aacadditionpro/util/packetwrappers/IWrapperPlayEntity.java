package de.photon.aacadditionpro.util.packetwrappers;

import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.World;
import org.bukkit.entity.Entity;

public interface IWrapperPlayEntity extends IWrapperPlay
{

    /**
     * Retrieve Entity ID.
     * <p>
     * Notes: entity's ID
     *
     * @return The current Entity ID
     */
    default int getEntityID()
    {
        return getHandle().getIntegers().read(0);
    }

    /**
     * Set Entity ID.
     *
     * @param value - new value.
     */
    default void setEntityID(int value)
    {
        getHandle().getIntegers().write(0, value);
    }

    /**
     * Retrieve the entity of the painting that will be spawned.
     *
     * @param world - the current world of the entity.
     *
     * @return The spawned entity.
     */
    default Entity getEntity(World world)
    {
        return getHandle().getEntityModifier(world).read(0);
    }

    /**
     * Retrieve the entity of the painting that will be spawned.
     *
     * @param event - the packet event.
     *
     * @return The spawned entity.
     */
    default Entity getEntity(PacketEvent event)
    {
        return getEntity(event.getPlayer().getWorld());
    }
}
