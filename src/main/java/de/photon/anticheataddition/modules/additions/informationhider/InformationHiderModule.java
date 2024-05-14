package de.photon.anticheataddition.modules.additions.informationhider;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;

public abstract class InformationHiderModule extends Module
{
    protected InformationHiderModule(String configString)
    {
        super(configString);
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

                    for (var equip : wrapper.getEquipment()) {
                        this.hideInformation(user, wrapper, entityId, equip.getItem());
                    }
                }).build();

        return ModuleLoader.builder(this)
                           .addPacketListeners(adapter)
                           .setAllowedServerVersions(ServerVersion.NON_188_VERSIONS)
                           .build();
    }

    protected abstract void hideInformation(User user, WrapperPlayServerEntityEquipment wrapper, int entityId, ItemStack item);
}
