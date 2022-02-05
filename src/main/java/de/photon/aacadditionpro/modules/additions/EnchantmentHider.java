package de.photon.aacadditionpro.modules.additions;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.protocol.PacketAdapterBuilder;
import de.photon.aacadditionpro.protocol.packetwrappers.sentbyserver.WrapperPlayServerEntityEquipment;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import lombok.val;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;

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

                    val wrapper = new WrapperPlayServerEntityEquipment(event.getPacket());
                    val entity = wrapper.getEntity(event);

                    // Do not modify the players' own enchantments.
                    if (entity.getEntityId() == user.getPlayer().getEntityId()) return;

                    val entityType = entity.getType();

                    // Only modify the packets if they originate from a player.
                    if (spoofPlayers && entityType == EntityType.PLAYER ||
                        spoofOthers && entityType != EntityType.PLAYER)
                    {
                        // Clone item to prevent a serversided connection of the equipment.
                        val item = wrapper.getItem().clone();
                        if (item.getEnchantments().isEmpty()) return;

                        // Clone the packet to prevent a serversided connection of the equipment.
                        event.setPacket(event.getPacket().deepClone());

                        for (Enchantment enchantment : item.getEnchantments().keySet()) {
                            item.removeEnchantment(enchantment);
                        }

                        item.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
                        wrapper.setItem(item);
                    }
                }).build();

        return ModuleLoader.builder(this)
                           .addPacketListeners(adapter)
                           .build();
    }
}
