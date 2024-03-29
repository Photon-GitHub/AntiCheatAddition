package de.photon.anticheataddition.modules.additions;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.protocol.EntityMetadataIndex;
import de.photon.anticheataddition.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.util.minecraft.world.entity.EntityUtil;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import org.bukkit.entity.*;

public final class DamageIndicator extends Module
{
    public static final DamageIndicator INSTANCE = new DamageIndicator();

    private final boolean spoofAnimals = loadBoolean(".spoof.animals", false);
    private final boolean spoofMonsters = loadBoolean(".spoof.monsters", true);
    private final boolean spoofPlayers = loadBoolean(".spoof.players", true);

    private DamageIndicator()
    {
        super("DamageIndicator");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.of(this, PacketAdapterBuilder
                .of(this, PacketType.Play.Server.ENTITY_METADATA, PacketType.Play.Server.UPDATE_HEALTH)
                .priority(PacketListenerPriority.HIGH)
                .onSending((event, user) -> {
                    if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
                        com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata(event);

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
                    }
                }).build());
    }
}