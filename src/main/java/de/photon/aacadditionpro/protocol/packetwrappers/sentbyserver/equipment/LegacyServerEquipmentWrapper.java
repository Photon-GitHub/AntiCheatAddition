package de.photon.aacadditionpro.protocol.packetwrappers.sentbyserver.equipment;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot;
import com.comphenix.protocol.wrappers.Pair;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.protocol.packetwrappers.AbstractPacket;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class LegacyServerEquipmentWrapper extends AbstractPacket implements IWrapperPlayEquipment
{
    public static final PacketType TYPE = PacketType.Play.Server.ENTITY_EQUIPMENT;

    private final List<Pair<ItemSlot, ItemStack>> slotStackPairs = new ArrayList<>();

    public LegacyServerEquipmentWrapper()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public LegacyServerEquipmentWrapper(final PacketContainer packet)
    {
        super(packet, TYPE);
        this.slotStackPairs.add(new Pair<>(this.getSlot(), this.getItem()));
    }

    @Override
    public List<Pair<ItemSlot, ItemStack>> getSlotStackPairs()
    {
        return slotStackPairs;
    }

    @Override
    public boolean setSlotStackPair(ItemSlot slot, ItemStack item)
    {
        boolean removed = slotStackPairs.removeIf(pair -> pair.getFirst().equals(slot));
        slotStackPairs.add(new Pair<>(slot, item));
        handle.getSlotStackPairLists().write(0, slotStackPairs);
        return removed;
    }

    @Override
    public boolean removeSlotStackPair(ItemSlot slot)
    {
        boolean removed = slotStackPairs.removeIf(pair -> pair.getFirst().equals(slot));
        handle.getSlotStackPairLists().write(0, slotStackPairs);
        return removed;
    }

    @Override
    public boolean isSlotSet(ItemSlot slot)
    {
        return slotStackPairs.stream().anyMatch(pair -> pair.getFirst().equals(slot));
    }

    @Override
    public ItemStack getItem(ItemSlot slot)
    {
        for (Pair<ItemSlot, ItemStack> pair : handle.getSlotStackPairLists().read(0)) {
            if (pair.getFirst().equals(slot)) {
                return pair.getSecond();
            }
        }
        return null;
    }

    @Override
    public void sendTranslatedPackets(Player receiver)
    {
        for (Pair<ItemSlot, ItemStack> pair : slotStackPairs) {
            LegacyServerEquipmentWrapper wrapper = new LegacyServerEquipmentWrapper();
            wrapper.setEntityID(this.getEntityID());
            wrapper.setSlot(pair.getFirst());
            wrapper.setItem(pair.getSecond());
            wrapper.sendPacket(receiver);
        }
    }

    private ItemSlot getSlot()
    {
        return handle.getItemSlots().read(0);
    }

    private void setSlot(final ItemSlot value)
    {
        // Player = null will return the server version.
        if (ServerVersion.is18()) {
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
    private ItemStack getItem()
    {
        return handle.getItemModifier().read(0);
    }

    /**
     * Set Item.
     *
     * @param value - new value.
     */
    private void setItem(final ItemStack value)
    {
        handle.getItemModifier().write(0, value);
    }
}