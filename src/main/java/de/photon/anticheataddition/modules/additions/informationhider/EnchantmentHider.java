package de.photon.anticheataddition.modules.additions.informationhider;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.Enchantment;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.log.Log;

import java.util.List;

public final class EnchantmentHider extends InformationHiderModule
{
    public static final EnchantmentHider INSTANCE = new EnchantmentHider();

    private EnchantmentHider()
    {
        super("InformationHider.parts.enchantments");
    }

    @Override
    protected void hideInformation(User user, WrapperPlayServerEntityEquipment wrapper, int entityId, ItemStack item)
    {
        Log.finer(() -> "EnchantmentHider entity id: " + entityId + " with item " + item);
        final ClientVersion version = PacketEvents.getAPI().getPlayerManager().getClientVersion(user.getPlayer());

        // Check if the item has enchantments
        final List<Enchantment> enchantments = item.getEnchantments(version);
        if (enchantments.isEmpty()) return;

        // If it has, clear them and add a dummy enchantment.
        Log.finer(() -> "Hiding enchantments of " + item + " for entity id " + entityId);
        enchantments.clear();
        enchantments.add(Enchantment.builder().type(EnchantmentTypes.INFINITY_ARROWS).level(1).build());
        item.setEnchantments(enchantments, version);
    }
}
