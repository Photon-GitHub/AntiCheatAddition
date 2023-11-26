package de.photon.anticheataddition.util.datastructure.kdtree;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.tinspin.index.PointMap;

/**
 * This is a KDTree specialized to handle {@link Player}s.
 * It uses
 */
public class Entity3DTree<T extends Entity> extends EntityPointMap<T>
{
    @Override
    public PointMap<T> createPointMap()
    {
        return PointMap.Factory.createKdTree(3);
    }

    @Override
    public double[] getPoint(T entity)
    {
        final var loc = entity.getLocation();
        return new double[]{loc.getX(), loc.getY(), loc.getZ()};
    }

    @Override
    public double[] getMin(T entity, double radius)
    {
        final var loc = entity.getLocation();
        return new double[]{loc.getX() - radius, loc.getY() - radius, loc.getZ() - radius};
    }

    @Override
    public double[] getMax(T entity, double radius)
    {
        final var loc = entity.getLocation();
        return new double[]{loc.getX() + radius, loc.getY() + radius, loc.getZ() + radius};
    }
}
