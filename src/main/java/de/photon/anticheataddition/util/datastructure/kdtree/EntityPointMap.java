package de.photon.anticheataddition.util.datastructure.kdtree;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tinspin.index.Index;
import org.tinspin.index.PointMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Abstract class representing a map of entities in a multidimensional data structure like kd-trees or quadtrees.
 * It implements Iterable to allow iteration over the entities.
 *
 * @param <T> the type of entity, which extends the Entity class.
 */
public abstract class EntityPointMap<T extends Entity> implements Iterable<Index.PointEntry<T>>
{

    // Internal PointMap to manage the entities.
    private final PointMap<T> pointMap = createPointMap();

    // Fixed iterator that can be reset to get some entity from the pointMap.
    private final Index.PointIterator<T> anyIterator = pointMap.iterator();

    /**
     * This method creates the PointMap.
     * Use {@link PointMap.Factory}'s methods to select a specific implementation.
     */
    public abstract PointMap<T> createPointMap();

    /**
     * Gets the point coordinates of an entity.
     *
     * @param entity the entity to get the point of.
     *
     * @return an array of doubles representing the point coordinates.
     */
    public abstract double[] getPoint(T entity);

    /**
     * Both getMin and getMax are used to create a bounding box around an entity to search for other entities in the methods
     * {@link #searchAroundAny(double)} and {@link #searchInRadius(Entity, double)}.
     *
     * @param entity the entity around which the minimum point is calculated.
     * @param radius the radius within which to calculate.
     *
     * @return an array of doubles representing the minimum point coordinates.
     */
    public abstract double[] getMin(T entity, double radius);

    /**
     * Both getMin and getMax are used to create a bounding box around an entity to search for other entities in the methods
     * {@link #searchAroundAny(double)} and {@link #searchInRadius(Entity, double)}.
     *
     * @param entity the entity around which the maximum point is calculated.
     * @param radius the radius within which to calculate.
     *
     * @return an array of doubles representing the minimum point coordinates.
     */
    public abstract double[] getMax(T entity, double radius);

    /**
     * Adds an entity to the PointMap.
     * The coordinates are calculated using {@link #getPoint(Entity)}.
     *
     * @param entity the entity to add.
     */
    public void add(T entity)
    {
        pointMap.insert(getPoint(entity), entity);
    }

    /**
     * Removes an entity from the PointMap.
     * The coordinates are calculated using {@link #getPoint(Entity)}.
     *
     * @param entity the entity to remove.
     */
    public void remove(T entity)
    {
        pointMap.remove(getPoint(entity));
    }

    /**
     * Searches for entities around a randomly chosen entity within a given radius and removes them from the PointMap.
     *
     * @param radius the search radius.
     *
     * @return a list of entities found and removed.
     */
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

    /**
     * Searches for entities around a randomly chosen entity in the PointMap within a given radius.
     *
     * @param radius the search radius.
     *
     * @return an iterator for the entities found, or null if no entity is found.
     */
    @Nullable
    public Index.PointIterator<T> searchAroundAny(double radius)
    {
        final var any = getAny();
        if (any == null) return null;
        return searchInRadius(getAny(), radius);
    }

    /**
     * Searches for entities within a radius around a specified center entity.
     *
     * @param center the entity to center the search around.
     * @param radius the search radius.
     *
     * @return an iterator for the entities found.
     */
    public Index.PointIterator<T> searchInRadius(T center, double radius)
    {
        return pointMap.query(getMin(center, radius), getMax(center, radius));
    }

    /**
     * Gets any entity from the PointMap, if available.
     *
     * @return an arbitrary entity from the PointMap or null if the PointMap is empty.
     */
    @Nullable
    public T getAny()
    {
        if (pointMap.size() == 0) return null;

        anyIterator.reset(null, null);
        return anyIterator.next().value();
    }

    /**
     * Returns the number of entities in the PointMap.
     *
     * @return the size of the PointMap.
     */
    public int size()
    {
        return pointMap.size();
    }

    /**
     * Checks if the PointMap is empty.
     *
     * @return true if the PointMap is empty, false otherwise.
     */
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
