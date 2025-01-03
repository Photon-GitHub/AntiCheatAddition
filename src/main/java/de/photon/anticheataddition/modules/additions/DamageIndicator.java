package de.photon.anticheataddition.modules.additions;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.util.protocol.EntityMetadataIndex;
import de.photon.anticheataddition.util.protocol.LivingEntityIdLookup;
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;
import org.bukkit.entity.Player;

public final class DamageIndicator extends Module
{
    public static final DamageIndicator INSTANCE = new DamageIndicator();

    private final boolean spoofOthers = loadBoolean(".spoof.others", true);
    private final boolean spoofPlayers = loadBoolean(".spoof.players", true);

    private DamageIndicator()
    {
        super("DamageIndicator");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.of(this, PacketAdapterBuilder
                // The UPDATE_HEALTH packet is only sent to the player themselves, therefore we don't need to handle it.
                .of(this, PacketType.Play.Server.ENTITY_METADATA)
                .priority(PacketListenerPriority.HIGH)
                .onSending((event, user) -> {
                    if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
                        final var wrapper = new WrapperPlayServerEntityMetadata(event);

                        final Player player = event.getPlayer();
                        final int entityId = wrapper.getEntityId();

                        // Player can get their own metadata.
                        if (player.getEntityId() == entityId) return;

                        // This is automatically cached.
                        final EntityType entityType = LivingEntityIdLookup.INSTANCE.getEntityType(entityId);
                        // Lookup failed, so the entity is not a living entity.
                        if (entityType == null) return;

                        // Bossbar problems
                        // Cannot use Boss interface as that doesn't exist on 1.8.8
                        if (entityType == EntityTypes.ENDER_DRAGON ||
                            entityType == EntityTypes.WITHER) return;

                        // Only spoof the entity if configured to do so.
                        if (entityType == EntityTypes.PLAYER ? !spoofPlayers : !spoofOthers) return;

                        for (EntityData data : wrapper.getEntityMetadata()) {
                            // Search for health.
                            if (data.getIndex() == EntityMetadataIndex.HEALTH
                                // Only modify alive entities (health > 0).
                                && ((Float) data.getValue() > 0)) {
                                data.setValue(0.5F);
                            }
                        }
                    }
                }).build());
    }
}