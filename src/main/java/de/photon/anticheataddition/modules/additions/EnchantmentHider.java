package de.photon.anticheataddition.modules.additions;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.wrappers.Pair;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.protocol.packetwrappers.sentbyserver.equipment.IWrapperPlayEquipment;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class EnchantmentHider extends Module
{
    public static final EnchantmentHider INSTANCE = new EnchantmentHider();

    private final boolean spoofPlayers = loadBoolean(".spoof.players", true);
    private final boolean spoofOthers = loadBoolean(".spoof.others", false);

    private EnchantmentHider()
    {
        super("EnchantmentHider");
    }

    private static void obfuscateEnchantments(IWrapperPlayEquipment wrapper)
    {
        for (final var pair : wrapper.getSlotStackPairs()) {
            final ItemStack stack = pair.getSecond();
            final Map<Enchantment, Integer> enchantments = stack.getEnchantments();

            if (enchantments.isEmpty()) continue;

            // Remove all enchantments.
            // The enchantments are an immutable map -> forEach.
            for (Enchantment enchantment : enchantments.keySet()) stack.removeEnchantment(enchantment);

            // Add dummy enchantment.
            stack.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
            wrapper.setSlotStackPair(pair.getFirst(), stack);
        }
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        final var adapter = PacketAdapterBuilder
                .of(this, PacketType.Play.Server.ENTITY_EQUIPMENT)
                .priority(ListenerPriority.HIGH)
                .onSending((event, user) -> {
                    final var wrapper = IWrapperPlayEquipment.of(event.getPacket());
                    final var entity = wrapper.getEntity(event);

                    // Do not modify the players' own enchantments.
                    if (entity == null || entity.getEntityId() == user.getPlayer().getEntityId()) return;

                    // Only modify the packets if they originate from a player.
                    if (entity.getType() == EntityType.PLAYER ? spoofPlayers : spoofOthers) {
                        // If all items do not have any enchantments, skip.
                        if (wrapper.getSlotStackPairs().stream().map(Pair::getSecond).map(ItemStack::getEnchantments).allMatch(Map::isEmpty)) return;

                        // Clone the packet to prevent a serversided connection of the equipment.
                        event.setPacket(event.getPacket().deepClone());
                        // The cloned packet needs a new wrapper!
                        obfuscateEnchantments(IWrapperPlayEquipment.of(event.getPacket()));
                    }
                }).build();

        return ModuleLoader.builder(this)
                           .addPacketListeners(adapter)
                           .setAllowedServerVersions(ServerVersion.NON_188_VERSIONS)
                           .build();
    }
}
