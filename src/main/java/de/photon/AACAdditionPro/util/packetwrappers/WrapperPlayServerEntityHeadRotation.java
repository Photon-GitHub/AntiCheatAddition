package de.photon.AACAdditionPro.util.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.World;
import org.bukkit.entity.Entity;

public class WrapperPlayServerEntityHeadRotation extends AbstractPacket
{
    public static final PacketType TYPE =
            PacketType.Play.Server.ENTITY_HEAD_ROTATION;

    public WrapperPlayServerEntityHeadRotation()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayServerEntityHeadRotation(PacketContainer packet)
    {
        super(packet, TYPE);
    }

    /**
     * Retrieve Entity ID.
     * <p>
     * Notes: entity's ID
     *
     * @return The current Entity ID
     */
    public int getEntityID()
    {
        return handle.getIntegers().read(0);
    }

    /**
     * Set Entity ID.
     *
     * @param value - new value.
     */
    public void setEntityID(int value)
    {
        handle.getIntegers().write(0, value);
    }

    /**
     * Retrieve the entity of the painting that will be spawned.
     *
     * @param world - the current world of the entity.
     *
     * @return The spawned entity.
     */
    public Entity getEntity(World world)
    {
        return handle.getEntityModifier(world).read(0);
    }

    /**
     * Retrieve the entity of the painting that will be spawned.
     *
     * @param event - the packet event.
     *
     * @return The spawned entity.
     */
    public Entity getEntity(PacketEvent event)
    {
        return getEntity(event.getPlayer().getWorld());
    }

    /**
     * Retrieve Head Yaw.
     * <p>
     * Notes: head yaw in steps of 2p/256
     *
     * @return The current Head Yaw
     */
    public byte getHeadYaw()
    {
        return handle.getBytes().read(0);
    }

    /**
     * Set Head Yaw.
     *
     * @param value - new value.
     */
    public void setHeadYaw(byte value)
    {
        handle.getBytes().write(0, value);
    }
}