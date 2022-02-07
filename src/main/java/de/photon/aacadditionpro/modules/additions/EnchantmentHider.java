package de.photon.aacadditionpro.modules.additions;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.protocol.PacketAdapterBuilder;
import de.photon.aacadditionpro.protocol.packetwrappers.sentbyserver.equipment.IWrapperPlayEquipment;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import lombok.val;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class EnchantmentHider extends Module
{
    @LoadFromConfiguration(configPath = ".spoof.players")
    private boolean spoofPlayers;
    @LoadFromConfiguration(configPath = ".spoof.others")
    private boolean spoofOthers;

    public EnchantmentHider()
    {
        super("EnchantmentHider");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        val adapter = PacketAdapterBuilder
                .of(PacketType.Play.Server.ENTITY_EQUIPMENT)
                .priority(ListenerPriority.HIGH)
                .onSending(event -> {
                    val user = User.safeGetUserFromPacketEvent(event);
                    if (User.isUserInvalid(user, this)) return;

                    var wrapper = IWrapperPlayEquipment.of(event.getPacket());
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
                        wrapper = IWrapperPlayEquipment.of(event.getPacket());

                        val pairs = wrapper.getSlotStackPairs();

                        // Remove all enchantments.
                        for (Pair<EnumWrappers.ItemSlot, ItemStack> pair : pairs) {
                            val stack = pair.getSecond();
                            val enchantments = stack.getEnchantments();

                            if (enchantments.isEmpty()) continue;

                            // Remove all enchantments.
                            enchantments.keySet().forEach(stack::removeEnchantment);

                            // Add dummy enchantment.
                            stack.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
                            wrapper.setSlotStackPair(pair.getFirst(), stack);
                        }
                    }
                }).build();

        return ModuleLoader.builder(this)
                           .addPacketListeners(adapter)
                           .build();
    }
}
