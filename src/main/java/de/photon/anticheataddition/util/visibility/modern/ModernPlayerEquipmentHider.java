package de.photon.anticheataddition.util.visibility.modern;

import com.comphenix.protocol.PacketType;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.util.visibility.PacketInformationHider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class ModernPlayerEquipmentHider extends PacketInformationHider
{
    private static final ItemStack AIR_STACK = new ItemStack(Material.AIR);

    public ModernPlayerEquipmentHider()
    {
        super(PacketType.Play.Server.ENTITY_EQUIPMENT);
    }

    @Override
    protected void onPreHide(@NotNull Player observer, @NotNull Set<Player> toHide)
    {
        for (Player player : toHide) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                observer.sendEquipmentChange(player, slot, AIR_STACK);
            }
        }
    }

    @Override
    protected void onReveal(@NotNull Player observer, @NotNull Set<Player> revealed)
    {
        for (Player watched : revealed) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                final var stack = watched.getInventory().getItem(slot);
                observer.sendEquipmentChange(watched, slot, stack == null ? AIR_STACK : stack);
            }
        }
    }

    @Override
    protected Set<ServerVersion> getSupportedVersions()
    {
        return ServerVersion.MC119.getSupVersionsFrom();
    }
}
