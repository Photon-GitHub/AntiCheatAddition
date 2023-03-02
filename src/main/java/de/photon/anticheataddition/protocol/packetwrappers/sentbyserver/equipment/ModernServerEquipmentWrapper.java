package de.photon.anticheataddition.protocol.packetwrappers.sentbyserver.equipment;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot;
import com.comphenix.protocol.wrappers.Pair;
import de.photon.anticheataddition.protocol.packetwrappers.AbstractPacket;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ModernServerEquipmentWrapper extends AbstractPacket implements IWrapperPlayEquipment
{
    public static final PacketType TYPE = PacketType.Play.Server.ENTITY_EQUIPMENT;

    public ModernServerEquipmentWrapper()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public ModernServerEquipmentWrapper(final PacketContainer packet)
    {
        super(packet, TYPE);
    }

    public List<Pair<ItemSlot, ItemStack>> getSlotStackPairs()
    {
        return handle.getSlotStackPairLists().read(0);
    }

    public boolean setSlotStackPair(ItemSlot slot, ItemStack item)
    {
        List<Pair<ItemSlot, ItemStack>> slotStackPairs = handle.getSlotStackPairLists().read(0);
        boolean removed = slotStackPairs.removeIf(pair -> pair.getFirst().equals(slot));
        slotStackPairs.add(new Pair<>(slot, item));
        handle.getSlotStackPairLists().write(0, slotStackPairs);
        return removed;
    }

    public boolean removeSlotStackPair(ItemSlot slot)
    {
        List<Pair<ItemSlot, ItemStack>> slotStackPairs = handle.getSlotStackPairLists().read(0);
        boolean removed = slotStackPairs.removeIf(pair -> pair.getFirst().equals(slot));
        handle.getSlotStackPairLists().write(0, slotStackPairs);
        return removed;
    }

    public boolean isSlotSet(ItemSlot slot)
    {
        return handle.getSlotStackPairLists().read(0).stream().map(Pair::getFirst).anyMatch(slot::equals);
    }

    public ItemStack getItem(ItemSlot slot)
    {
        for (Pair<ItemSlot, ItemStack> pair : handle.getSlotStackPairLists().read(0)) {
            if (pair.getFirst().equals(slot)) return pair.getSecond();
        }
        return null;
    }

    @Override
    public void sendTranslatedPackets(Player receiver)
    {
        this.sendPacket(receiver);
    }
}