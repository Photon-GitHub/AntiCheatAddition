package de.photon.aacadditionpro.util.minecraft.entity;

import de.photon.aacadditionpro.ServerVersion;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;

public interface EntityUtil
{
    EntityUtil INSTANCE = ServerVersion.containsActiveServerVersion(ServerVersion.MC115.getVersionsTo()) ? new LegacyEntityUtil() : new ModernEntityUtil();

    /**
     * Creates a {@link Predicate} that maps to <code>true</code> only for a certain {@link EntityType}.
     */
    default Predicate<Entity> ofType(EntityType type)
    {
        return entity -> entity.getType() == type;
    }

    /**
     * Determines if a {@link LivingEntity} is gliding (i.e. flying with an elytra)
     */
    boolean isFlyingWithElytra(@NotNull final LivingEntity livingEntity);

    /**
     * Gets the maximum health of an {@link LivingEntity}.
     */
    double getMaxHealth(@NotNull LivingEntity livingEntity);

    /**
     * Gets the passengers of an entity.
     * This method solves the compatibility issues of the newer APIs with server version 1.8.8
     */
    List<Entity> getPassengers(@NotNull final Entity entity);
}
