package de.photon.anticheataddition.util.datastructure.kdtree;

import org.bukkit.entity.Entity;
import org.tinspin.index.PointMap;

public class Entity2DTree<T extends Entity> extends EntityPointMap<T>
{
    @Override
    public PointMap<T> createPointMap()
    {
        return PointMap.Factory.createKdTree(2);
    }

    @Override
    public double[] getPoint(T entity)
    {
        final var loc = entity.getLocation();
        return new double[]{loc.getX(), loc.getZ()};
    }

    @Override
    public double[] getMin(T entity, double radius)
    {
        final var loc = entity.getLocation();
        return new double[]{loc.getX() - radius, loc.getZ() - radius};
    }

    @Override
    public double[] getMax(T entity, double radius)
    {
        final var loc = entity.getLocation();
        return new double[]{loc.getX() + radius, loc.getZ() + radius};
    }
}
