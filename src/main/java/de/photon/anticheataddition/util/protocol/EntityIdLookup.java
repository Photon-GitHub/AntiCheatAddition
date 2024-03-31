package de.photon.anticheataddition.util.protocol;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.entity.EntityType;

public class EntityIdLookup
{
    // Track at most 2 millon entities
    private static final long MAXIMUM_CACHED_ENTITIES = 1 << 21;
    private static final Cache<Integer, EntityType> ENTITY_ID_CACHE = CacheBuilder.newBuilder()
                                                                                  .initialCapacity(1 << 10)
                                                                                  .maximumSize(MAXIMUM_CACHED_ENTITIES)
                                                                                  .build();


    public static void cacheEntityId(int entityId, EntityType entityType)
    {
        ENTITY_ID_CACHE.put(entityId, entityType);
    }

    public static void getEntityType(int entityId)
    {
        ENTITY_ID_CACHE.getIfPresent(entityId);
    }
}
