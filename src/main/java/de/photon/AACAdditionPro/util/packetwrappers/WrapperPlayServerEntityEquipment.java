package de.photon.AACAdditionPro.util.packetwrappers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot;
import de.photon.AACAdditionPro.util.multiversion.ServerVersion;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

public class WrapperPlayServerEntityEquipment extends AbstractPacket
{
    public static final PacketType TYPE = PacketType.Play.Server.ENTITY_EQUIPMENT;

    public WrapperPlayServerEntityEquipment()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayServerEntityEquipment(final PacketContainer packet)
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
    public void setEntityID(final int value)
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
    public Entity getEntity(final World world)
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
    public Entity getEntity(final PacketEvent event)
    {
        return getEntity(event.getPlayer().getWorld());
    }

    public ItemSlot getSlot()
    {
        return handle.getItemSlots().read(0);
    }

    public void setSlot(final ItemSlot value)
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                handle.getIntegers().write(1, value.ordinal());
                break;
            case MC110:
            case MC111:
            case MC112:
                handle.getItemSlots().write(0, value);
                break;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    /**
     * Retrieve Item.
     * <p>
     * Notes: item in slot format
     *
     * @return The current Item
     */
    public ItemStack getItem()
    {
        return handle.getItemModifier().read(0);
    }

    /**
     * Set Item.
     *
     * @param value - new value.
     */
    public void setItem(final ItemStack value)
    {
        handle.getItemModifier().write(0, value);
    }
}