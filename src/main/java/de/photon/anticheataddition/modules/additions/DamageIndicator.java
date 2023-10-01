package de.photon.anticheataddition.modules.additions;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.protocol.EntityMetadataIndex;
import de.photon.anticheataddition.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.protocol.packetwrappers.MetadataPacket;
import de.photon.anticheataddition.protocol.packetwrappers.sentbyserver.WrapperPlayServerEntityMetadata;
import de.photon.anticheataddition.util.minecraft.world.entity.EntityUtil;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;

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
                .of(this, PacketType.Play.Server.ENTITY_METADATA)
                .priority(ListenerPriority.HIGH)
                .onSending((event, user) -> {
                    final var entity = event.getPacket().getEntityModifier(event.getPlayer().getWorld()).read(0);
                    // Clientside entities will be null in the world's entity list.
                    if (entity == null) return;

                    final var entityType = entity.getType();

                    // Entity has health to begin with.
                    if (!entityType.isAlive() ||
                        // Bossbar problems
                        // Cannot use Boss interface as that doesn't exist on 1.8.8
                        entityType == EntityType.ENDER_DRAGON ||
                        entityType == EntityType.WITHER ||

                        entityType == EntityType.PLAYER && !spoofPlayers ||
                        entity instanceof Monster && !spoofMonsters ||
                        entity instanceof Animals && !spoofAnimals ||

                        // Not the player himself.
                        // Offline mode servers have name-based UUIDs, so that should be no problem.
                        event.getPlayer().getEntityId() == entity.getEntityId() ||

                        // Entity has no passengers.
                        EntityUtil.INSTANCE.hasPassengers(entity)) {
                        return;
                    }

                    // Clone the packet to prevent a serversided connection of the health.
                    event.setPacket(event.getPacket().deepClone());

                    final MetadataPacket metadata = new WrapperPlayServerEntityMetadata(event.getPacket());

                    // Only set it if the entity is not yet dead to prevent problems on the clientside.
                    // Set the health to 1.0F as that is the default value.
                    metadata.modifyMetadataIndex(EntityMetadataIndex.HEALTH, health -> (Float) health > 0.0F ? 1.0F : 0.0F);
                }).build());
    }
}