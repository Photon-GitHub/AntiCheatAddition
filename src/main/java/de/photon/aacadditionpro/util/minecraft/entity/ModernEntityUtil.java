package de.photon.aacadditionpro.util.minecraft.entity;

import com.google.common.base.Preconditions;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

class ModernEntityUtil implements EntityUtil
{
    @Override
    public boolean isFlyingWithElytra(@NotNull LivingEntity livingEntity)
    {
        return livingEntity.isGliding();
    }

    @Override
    public double getMaxHealth(@NotNull LivingEntity livingEntity)
    {
        return Preconditions.checkNotNull(livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH), "Tried to get max health of an entity without health.").getValue();
    }

    @Override
    public List<Entity> getPassengers(@NotNull Entity entity)
    {
        return entity.getPassengers();
    }
}
