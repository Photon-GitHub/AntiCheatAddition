package de.photon.anticheataddition.util.minecraft.entity;

import de.photon.anticheataddition.ServerVersion;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public sealed interface EntityUtil permits LegacyEntityUtil, ModernEntityUtil
{
    EntityUtil INSTANCE = ServerVersion.MC115.activeIsEarlierOrEqual() ? new LegacyEntityUtil() : new ModernEntityUtil();

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
