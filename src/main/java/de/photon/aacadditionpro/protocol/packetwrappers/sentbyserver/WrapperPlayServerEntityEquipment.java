package de.photon.aacadditionpro.protocol.packetwrappers.sentbyserver;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.protocol.packetwrappers.AbstractPacket;
import de.photon.aacadditionpro.protocol.packetwrappers.IWrapperPlayEntity;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class WrapperPlayServerEntityEquipment extends AbstractPacket implements IWrapperPlayEntity
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
     * Sets all equipment slots of the entity to air for the observer.
     *
     * @param entityId the id of the {@link Entity} which slots should be cleared.
     * @param observer the {@link Player} who shall no longer see the equipment.
     */
    public static void clearAllSlots(int entityId, Player observer)
    {
        WrapperPlayServerEntityEquipment equipmentWrapper;
        for (final ItemSlot slot : ItemSlot.values()) {
            //Update the equipment with fake-packets
            equipmentWrapper = new WrapperPlayServerEntityEquipment();

            equipmentWrapper.setEntityID(entityId);
            equipmentWrapper.setItem(new ItemStack(Material.AIR));

            // 1.8.8 is automatically included as of the bukkit-handling, therefore server-version specific handling
            // as of the different server classes / enums and the null-removal above.
            equipmentWrapper.setSlot(slot);
            equipmentWrapper.sendPacket(observer);
        }
    }

    public ItemSlot getSlot()
    {
        return handle.getItemSlots().read(0);
    }

    public void setSlot(final ItemSlot value)
    {
        // Player = null will return the server version.
        if (ServerVersion.getActiveServerVersion() == ServerVersion.MC18) {
            int index = value.ordinal();

            // Reduce by one if index greater 0 as the offhand (index 1) doesn't exist.
            if (index > 0) --index;

            handle.getIntegers().write(1, index);
        } else handle.getItemSlots().write(0, value);
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