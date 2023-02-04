package de.photon.anticheataddition.util.visibility.legacy;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.wrappers.EnumWrappers;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.protocol.packetwrappers.sentbyserver.equipment.IWrapperPlayEquipment;
import de.photon.anticheataddition.util.inventory.InventoryUtil;
import de.photon.anticheataddition.util.visibility.PacketInformationHider;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public final class LegacyPlayerEquipmentHider extends PacketInformationHider
{
    private static final ItemStack AIR_STACK = new ItemStack(Material.AIR);

    public LegacyPlayerEquipmentHider()
    {
        super(PacketType.Play.Server.ENTITY_EQUIPMENT);
    }

    @Override
    protected void onPreHide(@NotNull Player observer, @NotNull Set<Player> toHide)
    {
        for (Entity entity : toHide) {
            final int entityId = entity.getEntityId();
            final var wrapper = IWrapperPlayEquipment.of();

            wrapper.setEntityID(entityId);
            for (EnumWrappers.ItemSlot slot : EnumWrappers.ItemSlot.values()) wrapper.setSlotStackPair(slot, AIR_STACK);
            wrapper.sendTranslatedPackets(observer);
        }
    }

    @Override
    protected void onReveal(@NotNull Player observer, @NotNull Set<Player> revealed)
    {
        for (Player watched : revealed) {
            // Basic wrapper stuff.
            final IWrapperPlayEquipment wrapper = IWrapperPlayEquipment.of();
            wrapper.setEntityID(watched.getEntityId());

            // Hand Stuff.
            final List<ItemStack> handItems = InventoryUtil.INSTANCE.getHandContents(watched);
            wrapper.setSlotStackPair(EnumWrappers.ItemSlot.MAINHAND, handItems.get(0));
            if (handItems.size() > 1) wrapper.setSlotStackPair(EnumWrappers.ItemSlot.OFFHAND, handItems.get(1));

            // Equipment stuff.
            wrapper.setSlotStackPair(EnumWrappers.ItemSlot.HEAD, watched.getInventory().getHelmet());
            wrapper.setSlotStackPair(EnumWrappers.ItemSlot.CHEST, watched.getInventory().getChestplate());
            wrapper.setSlotStackPair(EnumWrappers.ItemSlot.LEGS, watched.getInventory().getLeggings());
            wrapper.setSlotStackPair(EnumWrappers.ItemSlot.FEET, watched.getInventory().getBoots());

            // Send the packet.
            wrapper.sendTranslatedPackets(observer);
        }
    }

    @Override
    protected Set<ServerVersion> getSupportedVersions()
    {
        return ServerVersion.NON_188_VERSIONS;
    }
}