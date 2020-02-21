package de.photon.aacadditionpro.util.packetwrappers.server;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.util.exceptions.UnknownMinecraftVersion;
import de.photon.aacadditionpro.util.packetwrappers.AbstractPacket;
import de.photon.aacadditionpro.util.packetwrappers.IWrapperPlayEntity;
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

    public ItemSlot getSlot()
    {
        return handle.getItemSlots().read(0);
    }

    public void setSlot(final ItemSlot value)
    {
        // Player = null will return the server version.
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                int index = value.ordinal();

                // Reduce by one if index greater 0 as the offhand (index 1) doesn't exist.
                if (index > 0) {
                    index--;
                }

                handle.getIntegers().write(1, index);
                break;
            case MC112:
            case MC113:
            case MC114:
            case MC115:
                handle.getItemSlots().write(0, value);
                break;
            default:
                throw new UnknownMinecraftVersion();
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

    /**
     * Sets all equipment slots of the entity to air for the observer.
     *
     * @param entityId the id of the {@link Entity} which slots should be cleared.
     * @param observer the {@link Player} who shall no longer see the equipment.
     */
    public static void clearAllSlots(int entityId, Player observer)
    {
        for (final EnumWrappers.ItemSlot slot : EnumWrappers.ItemSlot.values()) {
            //Update the equipment with fake-packets
            final WrapperPlayServerEntityEquipment wrapperPlayServerEntityEquipment = new WrapperPlayServerEntityEquipment();

            wrapperPlayServerEntityEquipment.setEntityID(entityId);
            wrapperPlayServerEntityEquipment.setItem(new ItemStack(Material.AIR));

            // 1.8.8 is automatically included as of the bukkit-handling, therefore server-version specific handling
            // as of the different server classes / enums and the null-removal above.
            wrapperPlayServerEntityEquipment.setSlot(slot);
            wrapperPlayServerEntityEquipment.sendPacket(observer);
        }
    }
}