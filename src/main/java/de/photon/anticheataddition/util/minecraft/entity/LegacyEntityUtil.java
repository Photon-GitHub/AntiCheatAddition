package de.photon.anticheataddition.util.minecraft.entity;

import lombok.val;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("deprecation")
final class LegacyEntityUtil implements EntityUtil
{
    @Override
    public boolean isFlyingWithElytra(@NotNull LivingEntity livingEntity)
    {
        // No Elytra in 1.8
        return false;
    }

    @Override
    public double getMaxHealth(@NotNull LivingEntity livingEntity)
    {
        // No attribute method.
        return livingEntity.getMaxHealth();
    }

    @Override
    public List<Entity> getPassengers(@NotNull Entity entity)
    {
        val passenger = entity.getPassenger();
        return passenger == null ? List.of() : List.of(passenger);
    }
}
