package de.photon.AACAdditionPro.util.world;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public final class EntityUtils
{
    /**
     * Gets all {@link Player}s who are in a certain range of the given {@link Player}
     *
     * @param initialPlayer   the location from which the distance is measured
     * @param squaredDistance the squared distance of the maximum range between the two players to include the other {@link Player} in the result.
     *
     * @return a {@link List} of {@link Player}s who are in range.
     */
    public static List<Player> getNearbyPlayers(final Player initialPlayer, final double squaredDistance)
    {
        final List<Player> nearbyPlayers = new ArrayList<>(5);

        for (final Player player : initialPlayer.getWorld().getPlayers())
        {
            if (!initialPlayer.getUniqueId().equals(player.getUniqueId()) &&
                // Check coordinates
                MathUtils.areLocationsInRange(initialPlayer.getLocation(), player.getLocation(), squaredDistance))
            {
                nearbyPlayers.add(player);
            }
        }

        return nearbyPlayers;
    }

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
        final List<LivingEntity> initialLivingEntities;
        try
        {
            initialLivingEntities = Bukkit.getScheduler().callSyncMethod(AACAdditionPro.getInstance(), () -> initialPlayer.getWorld().getLivingEntities()).get();
        } catch (InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
            return Collections.emptyList();
        }

        final List<LivingEntity> nearbyLivingEntities = new ArrayList<>(5);

        for (final LivingEntity livingEntity : initialLivingEntities)
        {
            if (!initialPlayer.getUniqueId().equals(livingEntity.getUniqueId()) &&
                // Check coordinates
                MathUtils.areLocationsInRange(initialPlayer.getLocation(), livingEntity.getLocation(), x, y, z))
            {
                nearbyLivingEntities.add(livingEntity);
            }
        }

        return nearbyLivingEntities;
    }
}