package de.photon.anticheataddition.modules.additions;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateHealth;
import de.photon.anticheataddition.protocol.EntityMetadataIndex;
import de.photon.anticheataddition.util.minecraft.world.entity.EntityUtil;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import org.bukkit.entity.*;

public class DamageIndicatorNew extends PacketListenerAbstract
{
    private final boolean spoofAnimals = true;
    private final boolean spoofMonsters = true;
    private final boolean spoofPlayers = true;

    private final boolean spoofFood = true;

    @Override
    public void onPacketSend(PacketSendEvent event)
    {
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata(event);

            final Player player = (Player) event.getPlayer();
            final int entityId = wrapper.getEntityId();

            // Player can get their own metadata.
            if (player.getEntityId() == entityId) return;

            // This is automatically cached.
            final Entity entity = SpigotReflectionUtil.getEntityById(wrapper.getEntityId());
            // Lookup failed.
            if (entity == null) return;

            final EntityType entityType = entity.getType();

            // Entity has health to begin with.
            if (!entityType.isAlive() ||
                // Bossbar problems
                // Cannot use Boss interface as that doesn't exist on 1.8.8
                entityType == EntityType.ENDER_DRAGON ||
                entityType == EntityType.WITHER ||
                entityType == EntityType.PLAYER && !spoofPlayers ||

                entity instanceof Monster && !spoofMonsters ||
                entity instanceof Animals && !spoofAnimals ||

                // Entity has no passengers.
                EntityUtil.INSTANCE.hasPassengers(entity)) return;

            for (EntityData data : wrapper.getEntityMetadata()) {
                // Search for health.
                if (data.getIndex() == EntityMetadataIndex.HEALTH
                    // Only modify alive entities (health > 0).
                    && ((Float) data.getValue() > 0)) {
                    data.setValue(0.5F);
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Server.UPDATE_HEALTH) {
            WrapperPlayServerUpdateHealth packet = new WrapperPlayServerUpdateHealth(event);

            // TODO: Check if this works.
            if (packet.getHealth() > 0) packet.setHealth(0.5F);
            if (spoofFood) {
                if (packet.getFood() > 0) packet.setFood(20);
                if (packet.getFoodSaturation() > 0) packet.setFoodSaturation(20);
            }
        }
    }
}
