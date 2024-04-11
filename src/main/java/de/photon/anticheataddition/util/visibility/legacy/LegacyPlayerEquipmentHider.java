package de.photon.anticheataddition.util.visibility.legacy;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.util.inventory.InventoryUtil;
import de.photon.anticheataddition.util.messaging.Log;
import de.photon.anticheataddition.util.visibility.PacketInformationHider;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class LegacyPlayerEquipmentHider extends PacketInformationHider
{
    public LegacyPlayerEquipmentHider()
    {
        super(SupportedPacketTypes.ENTITY_EQUIPMENT);
    }

    @Override
    protected void onPreHide(@NotNull Player observer, @NotNull Set<Player> toHide)
    {
        for (Player player : toHide) {
            final var equipment = Arrays.stream(EquipmentSlot.values())
                                        .map(slot -> new Equipment(slot, ItemStack.EMPTY))
                                        .toList();

            sendEquipment(player, equipment);
            Log.finest(() -> "Player " + player.getName() + "'s equipment has been hidden from " + observer.getName());
        }
    }

    @Override
    protected void onReveal(@NotNull Player observer, @NotNull Set<Player> revealed)
    {
        for (Player watched : revealed) {
            final List<ItemStack> handItems = InventoryUtil.INSTANCE.getHandContents(watched).stream().map(SpigotConversionUtil::fromBukkitItemStack).toList();

            final List<Equipment> equipment = new ArrayList<>();
            equipment.add(new Equipment(EquipmentSlot.MAIN_HAND, handItems.get(0)));
            if (handItems.size() > 1) equipment.add(new Equipment(EquipmentSlot.OFF_HAND, handItems.get(1)));

            equipment.add(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(watched.getInventory().getHelmet())));
            equipment.add(new Equipment(EquipmentSlot.CHEST_PLATE, SpigotConversionUtil.fromBukkitItemStack(watched.getInventory().getChestplate())));
            equipment.add(new Equipment(EquipmentSlot.LEGGINGS, SpigotConversionUtil.fromBukkitItemStack(watched.getInventory().getLeggings())));
            equipment.add(new Equipment(EquipmentSlot.BOOTS, SpigotConversionUtil.fromBukkitItemStack(watched.getInventory().getBoots())));

            sendEquipment(watched, equipment);

            Log.finest(() -> "Player " + watched.getName() + "'s equipment has been revealed to " + observer.getName());
        }
    }

    private static void sendEquipment(Player player, List<Equipment> equipment)
    {
        final int entityId = player.getEntityId();
        final var wrapper = new WrapperPlayServerEntityEquipment(entityId, equipment);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, wrapper);
    }

    @Override
    protected Set<ServerVersion> getSupportedVersions()
    {
        return ServerVersion.NON_188_VERSIONS;
    }
}