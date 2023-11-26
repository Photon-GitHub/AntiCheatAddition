package de.photon.anticheataddition.util.datastructure.kdtree;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tinspin.index.Index;
import org.tinspin.index.PointMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class EntityPointMap<T extends Entity> implements Iterable<Index.PointEntry<T>>
{
    private final PointMap<T> pointMap = createPointMap();
    private final Index.PointIterator<T> anyIterator = pointMap.iterator();

    public abstract PointMap<T> createPointMap();

    public abstract double[] getPoint(T entity);

    public abstract double[] getMin(T entity, double radius);

    public abstract double[] getMax(T entity, double radius);

    public void add(T entity)
    {
        pointMap.insert(getPoint(entity), entity);
    }

    public void remove(T entity)
    {
        pointMap.remove(getPoint(entity));
    }

    @NotNull
    public List<T> searchAroundAnyAndRemove(double radius)
    {
        final List<T> list = new ArrayList<>();
        final var iter = searchAroundAny(radius);
        if (iter == null) return list;

        Index.PointEntry<T> next;
        while (iter.hasNext()) {
            next = iter.next();
            list.add(next.value());
            pointMap.remove(next.point());
        }
        return list;
    }

    @Nullable
    public Index.PointIterator<T> searchAroundAny(double radius)
    {
        final var any = getAny();
        if (any == null) return null;
        return searchInRadius(getAny(), radius);
    }

    public Index.PointIterator<T> searchInRadius(T center, double radius)
    {
        return pointMap.query(getMin(center, radius), getMax(center, radius));
    }

    @Nullable
    public T getAny()
    {
        if (pointMap.size() == 0) return null;

        anyIterator.reset(null, null);
        return anyIterator.next().value();
    }

    public int size()
    {
        return pointMap.size();
    }

    public boolean isEmpty()
    {
        return pointMap.size() == 0;
    }

    @Override
    @NotNull
    public Iterator<Index.PointEntry<T>> iterator()
    {
        return pointMap.iterator();
    }
}
