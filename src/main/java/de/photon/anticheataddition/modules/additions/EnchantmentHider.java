package de.photon.anticheataddition.modules.additions;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.item.enchantment.Enchantment;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.protocol.PacketAdapterBuilder;

import java.util.List;

public final class EnchantmentHider extends Module
{
    public static final EnchantmentHider INSTANCE = new EnchantmentHider();

    private EnchantmentHider()
    {
        super("EnchantmentHider");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        final var adapter = PacketAdapterBuilder
                .of(this, PacketType.Play.Server.ENTITY_EQUIPMENT)
                .priority(PacketListenerPriority.HIGH)
                .onSending((event, user) -> {
                    final var wrapper = new WrapperPlayServerEntityEquipment(event);
                    final int entityId = wrapper.getEntityId();

                    // Do not modify the player's own equipment.
                    if (user.getPlayer().getEntityId() == entityId) return;

                    final ClientVersion version = PacketEvents.getAPI().getPlayerManager().getClientVersion(user.getPlayer());

                    for (var equip : wrapper.getEquipment()) {
                        final var item = equip.getItem();

                        // Check if the item has enchantments
                        final List<Enchantment> enchantments = item.getEnchantments(version);
                        if (enchantments.isEmpty()) continue;

                        // If it has, clear them and add a dummy enchantment.
                        enchantments.clear();
                        enchantments.add(Enchantment.builder().type(EnchantmentTypes.INFINITY_ARROWS).level(1).build());
                        item.setEnchantments(enchantments, version);
                    }
                }).build();

        return ModuleLoader.builder(this)
                           .addPacketListeners(adapter)
                           .setAllowedServerVersions(ServerVersion.NON_188_VERSIONS)
                           .build();
    }
}
