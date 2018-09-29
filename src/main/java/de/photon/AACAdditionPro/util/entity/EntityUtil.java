package de.photon.AACAdditionPro.util.entity;

import de.photon.AACAdditionPro.ServerVersion;
import de.photon.AACAdditionPro.util.mathematics.AxisAlignedBB;
import de.photon.AACAdditionPro.util.mathematics.Hitbox;
import de.photon.AACAdditionPro.util.world.BlockUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class EntityUtil
{
    /**
     * Determines if a {@link LivingEntity} is gliding (i.e. flying with an elytra)
     */
    public static boolean isFlyingWithElytra(final LivingEntity livingEntity)
    {
        // On 1.8.8 there is no Elytra
        return ServerVersion.getActiveServerVersion() != ServerVersion.MC188 &&
               // On higher versions check for gliding.
               livingEntity.isGliding();
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
    public static List<LivingEntity> getLivingEntitiesAroundPlayer(final Entity entity, final Hitbox hitbox, final double offset)
    {
        return getLivingEntitiesAroundPlayer(entity, hitbox.getOffsetX() + offset, hitbox.getHeight() + offset, hitbox.getOffsetZ() + offset);
    }

    /**
     * Gets all {@link LivingEntity}s around an {@link Entity}
     *
     * @param entity the entity from which the distance is measured
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

    /**
     * Gets all the {@link Material}s inside a {@link Hitbox} at a certain {@link Location} and adds them to a
     * {@link Set}.
     */
    public static Set<Material> getMaterialsInHitbox(final Location location, final Hitbox hitbox)
    {
        final Set<Material> materials = new HashSet<>();

        final AxisAlignedBB axisAlignedBB = hitbox.constructBoundingBox(location);
        int xMin = (int) axisAlignedBB.getMinX();
        int yMin = (int) axisAlignedBB.getMinY();
        int zMin = (int) axisAlignedBB.getMinZ();

        // Add 1 to ceil the value as the cast to int floors it.
        int xMax = ((int) axisAlignedBB.getMaxX()) + 1;
        int yMax = ((int) axisAlignedBB.getMaxY()) + 1;
        int zMax = ((int) axisAlignedBB.getMaxZ()) + 1;


        for (; xMin <= xMax; xMin++)
        {
            for (; yMin <= yMax; yMin++)
            {
                for (; zMin <= zMax; zMin++)
                {
                    materials.add(location.getWorld().getBlockAt(xMin, yMin, zMin).getType());
                }
            }
        }

        return materials;
    }

    /**
     * Checks if a {@link Hitbox} at a certain {@link Location} is inside liquids.
     *
     * @param location the {@link Location} to base the {@link Hitbox} on.
     * @param hitbox   the type of {@link Hitbox} that should be constructed.
     */
    public static boolean isHitboxInLiquids(final Location location, final Hitbox hitbox)
    {
        return isHitboxInMaterials(location, hitbox, BlockUtils.LIQUIDS);
    }

    /**
     * Checks if a {@link Hitbox} at a certain {@link Location} is inside of one of the provided {@link Material}s.
     *
     * @param location  the {@link Location} to base the {@link Hitbox} on.
     * @param hitbox    the type of {@link Hitbox} that should be constructed.
     * @param materials the {@link Material}s that should be checked for.
     */
    public static boolean isHitboxInMaterials(final Location location, final Hitbox hitbox, final Collection<Material> materials)
    {
        if (materials.isEmpty())
        {
            return false;
        }

        final AxisAlignedBB axisAlignedBB = hitbox.constructBoundingBox(location);
        int xMin = (int) axisAlignedBB.getMinX();
        int yMin = (int) axisAlignedBB.getMinY();
        int zMin = (int) axisAlignedBB.getMinZ();

        // Add 1 to ceil the value as the cast to int floors it.
        int xMax = ((int) axisAlignedBB.getMaxX()) + 1;
        int yMax = ((int) axisAlignedBB.getMaxY()) + 1;
        int zMax = ((int) axisAlignedBB.getMaxZ()) + 1;


        for (; xMin <= xMax; xMin++)
        {
            for (; yMin <= yMax; yMin++)
            {
                for (; zMin <= zMax; zMin++)
                {
                    if (materials.contains(location.getWorld().getBlockAt(xMin, yMin, zMin).getType()))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}