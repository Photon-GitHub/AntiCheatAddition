package de.photon.AACAdditionPro.util.entity;

import de.photon.AACAdditionPro.ServerVersion;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class EntityUtils
{
    /**
     * Determines if a {@link LivingEntity} is gliding (i.e. flying with an elytra)
     */
    public static boolean isFlyingWithElytra(final LivingEntity livingEntity)
    {
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                return false;
            case MC111:
            case MC112:
                return livingEntity.isGliding();
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    /**
     * Gets all {@link LivingEntity}s around an {@link Entity}
     *
     * @param entity the location from which the distance is measured
     * @param x      the maximum x-distance between the initialPlayer and the checked {@link Player} to add the checked {@link Player} to the {@link List}.
     * @param y      the maximum y-distance between the initialPlayer and the checked {@link Player} to add the checked {@link Player} to the {@link List}.
     * @param z      the maximum z-distance between the initialPlayer and the checked {@link Player} to add the checked {@link Player} to the {@link List}.
     *
     * @return a {@link List} of {@link LivingEntity}s which are in range.
     */
    public static List<LivingEntity> getLivingEntitiesAroundPlayer(final Entity entity, final double x, final double y, final double z)
    {
        final List<Entity> nearbyEntities = entity.getNearbyEntities(x, y, z);

        // nearbyLivingEntities must be smaller or equal to nearbyEntities in the end.
        final List<LivingEntity> nearbyLivingEntities = new ArrayList<>(nearbyEntities.size());

        for (Entity nearbyEntity : nearbyEntities)
        {
            if (nearbyEntity instanceof LivingEntity)
            {
                nearbyLivingEntities.add((LivingEntity) nearbyEntity);
            }
        }

        return nearbyLivingEntities;
    }
}