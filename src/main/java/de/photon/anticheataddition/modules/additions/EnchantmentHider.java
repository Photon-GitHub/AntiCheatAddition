package de.photon.anticheataddition.modules.additions;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.wrappers.Pair;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.protocol.packetwrappers.sentbyserver.equipment.IWrapperPlayEquipment;
import de.photon.anticheataddition.user.User;
import lombok.val;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class EnchantmentHider extends Module
{
    private final boolean spoofPlayers = loadBoolean(".spoof.players", true);
    private final boolean spoofOthers = loadBoolean(".spoof.others", false);

    public EnchantmentHider()
    {
        super("EnchantmentHider");
    }

    private static void obfuscateEnchantments(IWrapperPlayEquipment wrapper)
    {
        for (val pair : wrapper.getSlotStackPairs()) {
            final ItemStack stack = pair.getSecond();
            val enchantments = stack.getEnchantments();

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
        val adapter = PacketAdapterBuilder
                .of(PacketType.Play.Server.ENTITY_EQUIPMENT)
                .priority(ListenerPriority.HIGH)
                .onSending(event -> {
                    val user = User.safeGetUserFromPacketEvent(event);
                    if (event.isCancelled() || User.isUserInvalid(user, this)) return;

                    val wrapper = IWrapperPlayEquipment.of(event.getPacket());
                    val entity = wrapper.getEntity(event);

                    // Do not modify the players' own enchantments.
                    if (entity.getEntityId() == user.getPlayer().getEntityId()) return;

                    val entityType = entity.getType();

                    // Only modify the packets if they originate from a player.
                    if (spoofPlayers && entityType == EntityType.PLAYER ||
                        spoofOthers && entityType != EntityType.PLAYER)
                    {
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
