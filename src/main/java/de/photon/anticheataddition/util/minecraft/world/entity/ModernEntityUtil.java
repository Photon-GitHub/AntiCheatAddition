package de.photon.anticheataddition.util.minecraft.world.entity;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class ModernEntityUtil implements EntityUtil
{
    @Override
    public boolean isFlyingWithElytra(@NotNull LivingEntity livingEntity)
    {
        return livingEntity.isGliding();
    }

    @Override
    public List<Entity> getPassengers(@NotNull Entity entity)
    {
        return entity.getPassengers();
    }
}
