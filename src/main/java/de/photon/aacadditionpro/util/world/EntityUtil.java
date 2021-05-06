package de.photon.aacadditionpro.util.world;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.util.mathematics.Hitbox;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EntityUtil
{
    /**
     * Creates a {@link Predicate} that maps to <code>true</code> only for a certain {@link EntityType}.
     */
    public static Predicate<Entity> ofType(EntityType type)
    {
        return entity -> entity.getType() == type;
    }

    /**
     * Determines if a {@link LivingEntity} is gliding (i.e. flying with an elytra)
     */
    public static boolean isFlyingWithElytra(final LivingEntity livingEntity)
    {
        // On 1.8.8 there is no Elytra, on higher versions check for gliding.
        return ServerVersion.getActiveServerVersion() != ServerVersion.MC18 && livingEntity.isGliding();
    }

    /**
     * Gets all {@link LivingEntity}s around an {@link Entity}
     *
     * @param entity the entity from which the distance is measured
     * @param hitbox the {@link Hitbox} of the entity
     * @param offset additional distance from the hitbox in all directions
     *
     * @return a {@link List} of {@link LivingEntity}s which are in range.
     */
    public static List<LivingEntity> getLivingEntitiesAroundEntity(final Entity entity, final Hitbox hitbox, final double offset)
    {
        return getLivingEntitiesAroundEntity(entity, hitbox.getOffsetX() + offset, hitbox.getHeight() + offset, hitbox.getOffsetZ() + offset);
    }

    /**
     * Gets all {@link LivingEntity}s around an {@link Entity}
     *
     * @param entity the entity from which the distance is measured
     * @param x      the maximum x-distance between the initialPlayer and the checked {@link Player} to add the checked {@link Player} to the {@link List}.
     * @param y      the maximum y-distance between the initialPlayer and the checked {@link Player} to add the checked {@link Player} to the {@link List}.
     * @param z      the maximum z-distance between the initialPlayer and the checked {@link Player} to add the checked {@link Player} to the {@link List}.
     *
     * @return a {@link List} of {@link LivingEntity}s which are in range, excluding the given entity.
     */
    public static List<LivingEntity> getLivingEntitiesAroundEntity(final Entity entity, final double x, final double y, final double z)
    {
        // Streaming here as the returned list of getNearbyEntities is unmodifiable, therefore streaming reduces code
        // complexity.
        return entity.getNearbyEntities(x, y, z).stream()
                     .filter(e -> (e instanceof LivingEntity))
                     .map(LivingEntity.class::cast)
                     .collect(Collectors.toList());
    }

    /**
     * Gets the maximum health of an {@link LivingEntity}.
     */
    public static double getMaxHealth(LivingEntity livingEntity)
    {
        return ServerVersion.getActiveServerVersion() == ServerVersion.MC18 ?
               livingEntity.getMaxHealth() :
               Preconditions.checkNotNull(livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH), "Tried to get max health of an entity without health.").getValue();
    }

    /**
     * Gets the passengers of an entity.
     * This method solves the compatibility issues of the newer APIs with server version 1.8.8
     */
    public static List<Entity> getPassengers(final Entity entity)
    {
        if (ServerVersion.getActiveServerVersion() == ServerVersion.MC18) {
            val passenger = entity.getPassenger();
            return passenger == null ?
                   Collections.emptyList() :
                   Collections.singletonList(passenger);
        }
        return entity.getPassengers();
    }
}
