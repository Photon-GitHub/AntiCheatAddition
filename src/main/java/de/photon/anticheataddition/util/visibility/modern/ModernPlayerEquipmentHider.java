package de.photon.anticheataddition.util.visibility.modern;

import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.util.messaging.Log;
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
        super(SupportedPacketTypes.ENTITY_EQUIPMENT);
    }

    @Override
    protected void onPreHide(@NotNull Player observer, @NotNull Set<Player> toHide)
    {
        for (Player player : toHide) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                observer.sendEquipmentChange(player, slot, AIR_STACK);
                Log.finest(() -> "Player " + player.getName() + "'s equipment has been hidden from " + observer.getName());
            }
        }
    }

    @Override
    protected void onReveal(@NotNull Player observer, @NotNull Set<Player> revealed)
    {
        for (Player watched : revealed) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                final var stack = watched.getInventory().getItem(slot);
                // Only send non-null stacks, as the null stacks are air, and we sent that upon hiding.
                if (stack != null) observer.sendEquipmentChange(watched, slot, stack);
            }
            Log.finest(() -> "Player " + watched.getName() + "'s equipment has been revealed to " + observer.getName());
        }
    }

    @Override
    protected Set<ServerVersion> getSupportedVersions()
    {
        // Only in 1.19 the new sendEquipmentChange methods were added.
        return ServerVersion.MC119.getSupVersionsFrom();
    }
}
