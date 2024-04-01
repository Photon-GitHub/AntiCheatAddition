package de.photon.anticheataddition.util.protocol;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

import javax.annotation.Nullable;

public class LivingEntityIdLookup
{
    // Track at most 1 millon entities
    private static final long MAXIMUM_CACHE_CAPACITY = 1 << 20;
    private static final int MINIMUM_CACHE_CAPACITY = 1 << 11;

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
                    final WrapperPlayServerSpawnEntity wrapper = new WrapperPlayServerSpawnEntity(event);
                    cacheEntityId(wrapper.getEntityId(), wrapper.getEntityType());
                }
            }
        });

        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) cacheEntity(entity);
        }
    }

    private void cacheEntity(LivingEntity entity)
    {
        this.cacheEntityId(entity.getEntityId(), EntityTypes.getByName(entity.getType().name()));
    }

    public void cacheEntityId(int entityId, EntityType entityType)
    {
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