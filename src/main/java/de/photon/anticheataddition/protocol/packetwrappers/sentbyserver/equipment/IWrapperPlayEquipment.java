package de.photon.anticheataddition.protocol.packetwrappers.sentbyserver.equipment;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.protocol.packetwrappers.IWrapperPlayEntity;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface IWrapperPlayEquipment extends IWrapperPlayEntity
{
    ItemStack AIR_STACK = new ItemStack(Material.AIR);

    static IWrapperPlayEquipment of()
    {
        return ServerVersion.ACTIVE.compareTo(ServerVersion.MC116) < 0 ? new LegacyServerEquipmentWrapper() : new ModernServerEquipmentWrapper();
    }

    static IWrapperPlayEquipment of(final PacketContainer packet)
    {
        return ServerVersion.ACTIVE.compareTo(ServerVersion.MC116) < 0 ? new LegacyServerEquipmentWrapper(packet) : new ModernServerEquipmentWrapper(packet);
    }

    /**
     * Sets all equipment slots of the entity to air for the observer.
     *
     * @param entityId the id of the {@link Entity} which slots should be cleared.
     * @param observer the {@link Player} who shall no longer see the equipment.
     */
    static void clearAllSlots(int entityId, Player observer)
    {
        IWrapperPlayEquipment wrapper = of();

        wrapper.setEntityID(entityId);
        for (EnumWrappers.ItemSlot slot : EnumWrappers.ItemSlot.values()) wrapper.setSlotStackPair(slot, AIR_STACK);
        wrapper.sendTranslatedPackets(observer);
    }

    /**
     * Retrieve list of ItemSlot - ItemStack pairs.
     *
     * @return The current list of ItemSlot - ItemStack pairs.
     */
    List<Pair<EnumWrappers.ItemSlot, ItemStack>> getSlotStackPairs();

    /**
     * Set a ItemSlot - ItemStack pair.
     *
     * @param slot The slot the item will be equipped in. If matches an existing pair, will overwrite the old one
     * @param item The item to equip
     *
     * @return Whether a pair was overwritten.
     */
    boolean setSlotStackPair(EnumWrappers.ItemSlot slot, ItemStack item);

    /**
     * Removes the ItemSlot ItemStack pair matching the provided slot. If doesn't exist does nothing
     *
     * @param slot the slot to remove the pair from
     *
     * @return Whether a pair was removed.
     */
    boolean removeSlotStackPair(EnumWrappers.ItemSlot slot);

    /**
     * Check whether the provided is to be affected
     *
     * @param slot the slot to check for
     *
     * @return true if is set, false otherwise
     */
    boolean isSlotSet(EnumWrappers.ItemSlot slot);

    /**
     * Get the item being equipped to the provided slot
     *
     * @param slot the slot to retrieve the item from
     *
     * @return the equipping item, or null if doesn't exist
     */
    ItemStack getItem(EnumWrappers.ItemSlot slot);

    void sendTranslatedPackets(Player receiver);
}
