package de.photon.anticheataddition.protocol.packetwrappers.sentbyclient;


import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerAction;
import de.photon.anticheataddition.protocol.packetwrappers.AbstractPacket;
import org.bukkit.World;
import org.bukkit.entity.Entity;

public class WrapperPlayClientEntityAction extends AbstractPacket
{
    public static final PacketType TYPE = PacketType.Play.Client.ENTITY_ACTION;

    public WrapperPlayClientEntityAction()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayClientEntityAction(PacketContainer packet)
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
     * Retrieve Action ID.
     * <p>
     * Notes: the ID of the action, see below.
     *
     * @return The current Action ID
     */
    public PlayerAction getAction()
    {
        return handle.getPlayerActions().read(0);
    }

    /**
     * Set Action ID.
     *
     * @param value - new value.
     */
    public void setAction(PlayerAction value)
    {
        handle.getPlayerActions().write(0, value);
    }

    /**
     * Retrieve Jump Boost.
     * <p>
     * Notes: horse jump boost. Ranged from 0 -> 100.
     *
     * @return The current Jump Boost
     */
    public int getJumpBoost()
    {
        return handle.getIntegers().read(1);
    }

    /**
     * Set Jump Boost.
     *
     * @param value - new value.
     */
    public void setJumpBoost(int value)
    {
        handle.getIntegers().write(1, value);
    }

}