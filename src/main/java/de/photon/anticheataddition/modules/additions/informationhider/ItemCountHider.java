package de.photon.anticheataddition.modules.additions.informationhider;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;

public final class ItemCountHider extends Module
{
    public static final ItemCountHider INSTANCE = new ItemCountHider();

    private ItemCountHider()
    {
        super("InformationHider.parts.item_count");
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
                        final var item = equip.getItem();

                        if (item.getAmount() <= 1) continue;
                        item.setAmount(1);
                    }
                }).build();

        return ModuleLoader.builder(this)
                           .addPacketListeners(adapter)
                           .setAllowedServerVersions(ServerVersion.NON_188_VERSIONS)
                           .build();
    }
}
