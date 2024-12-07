package de.photon.anticheataddition.util.minecraft.world.entity;

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
    public List<Entity> getPassengers(@NotNull Entity entity)
    {
        final var passenger = entity.getPassenger();
        return passenger == null ? List.of() : List.of(passenger);
    }
}
