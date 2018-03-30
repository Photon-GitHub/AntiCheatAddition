package de.photon.AACAdditionPro.util.world;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class EntityUtils
{
    /**
     * Gets all {@link Player}s who are in a certain range of the given {@link Player}
     *
     * @param initialPlayer the location from which the distance is measured
     * @param x             the maximum x-distance between the initialPlayer and the checked {@link Player} to add the checked {@link Player} to the {@link List}.
     * @param y             the maximum y-distance between the initialPlayer and the checked {@link Player} to add the checked {@link Player} to the {@link List}.
     * @param z             the maximum z-distance between the initialPlayer and the checked {@link Player} to add the checked {@link Player} to the {@link List}.
     *
     * @return a {@link List} of {@link Player}s who are in range.
     */
    public static List<LivingEntity> getLivingEntitiesAroundPlayer(final Player initialPlayer, final double x, final double y, final double z)
    {
        final List<Entity> nearbyEntities = initialPlayer.getNearbyEntities(x, y, z);
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