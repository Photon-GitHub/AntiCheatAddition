package de.photon.anticheataddition.util.protocol;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.util.log.Log;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

import javax.annotation.Nullable;

public final class LivingEntityIdLookup
{
    // Track at most 1 millon entities
    private static final long MAXIMUM_CACHE_CAPACITY = 1 << 20;
    private static final int MINIMUM_CACHE_CAPACITY = 1 << 11;
    private static final long CACHE_TIME = 20L * 5L;

    public static final LivingEntityIdLookup INSTANCE = new LivingEntityIdLookup();

    private final Cache<Integer, EntityType> entityTypeCache = CacheBuilder.newBuilder().initialCapacity(MINIMUM_CACHE_CAPACITY).concurrencyLevel(2).maximumSize(MAXIMUM_CACHE_CAPACITY).build();


    private LivingEntityIdLookup()
    {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract()
        {
            @Override
            public void onPacketSend(PacketSendEvent event)
            {
                if (event.getPacketType() == PacketType.Play.Server.SPAWN_LIVING_ENTITY) {
                    final WrapperPlayServerSpawnLivingEntity wrapper = new WrapperPlayServerSpawnLivingEntity(event);
                    cacheEntityId(wrapper.getEntityId(), wrapper.getEntityType());
                }
            }
        });

        // Do not immediately loop through all entities to cache them, as Bukkit may not have loaded all entities yet which leads to connection errors when players try to join.
        Bukkit.getScheduler().scheduleSyncDelayedTask(AntiCheatAddition.getInstance(), this::cacheAllEntities, CACHE_TIME);
    }

    private void cacheAllEntities()
    {
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) cacheEntity(entity);
        }
    }

    private void cacheEntity(LivingEntity entity)
    {
        final int entityId = entity.getEntityId();
        final var entityType = SpigotConversionUtil.fromBukkitEntityType(entity.getType());

        if (entityType == null) {
            Log.fine(() -> "Attempted to cache null entityType: Entity ID " + entityId + " Raw type: " + entity.getType().name());
            return;
        }

        entityTypeCache.put(entityId, entityType);
    }

    public void cacheEntityId(int entityId, EntityType entityType)
    {
        if (entityType == null) {
            Log.fine(() -> "Attempted to cache null entityType: Entity ID " + entityId);
            return;
        }

        entityTypeCache.put(entityId, entityType);
    }

    /**
     * Gets the EntityType from a valid entityId of a living entity.
     */
    @Nullable
    public EntityType getEntityType(int entityId)
    {
        return entityTypeCache.getIfPresent(entityId);
    }
}