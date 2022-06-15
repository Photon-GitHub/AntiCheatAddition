package de.photon.anticheataddition.util.datastructure.kdtree;

import de.photon.anticheataddition.util.mathematics.MathUtil;
import lombok.val;
import org.danilopianini.util.FlexibleQuadTree;
import org.danilopianini.util.SpatialIndex;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public abstract class QuadTreeCollection<T> extends AbstractCollection<QuadTreeCollection.Node<T>> implements Collection<QuadTreeCollection.Node<T>>
{
    protected final SpatialIndex<Node<T>> quadTree = new FlexibleQuadTree<>();

    protected abstract Collection<Node<T>> getBackingCollection();

    @Override
    public boolean add(Node<T> node)
    {
        this.add(node.x, node.y, node.element);
        return true;
    }

    public void add(double x, double y, T element)
    {
        final Node<T> node = new Node<>(x, y, element);
        quadTree.insert(node, x, y);
        getBackingCollection().add(node);
    }

    /**
     * Gets any element from the {@link QuadTreeCollection} or null if empty.
     */
    public abstract Node<T> getAny();

    /**
     * Removes any element from the {@link QuadTreeCollection} or null if empty.
     */
    public abstract Node<T> removeAny();

    @Override
    public boolean remove(Object o)
    {
        return o instanceof Node<?> && this.remove((Node<T>) o);
    }

    public boolean remove(Node<T> node)
    {
        quadTree.remove(node, node.x, node.y);
        return getBackingCollection().remove(node);
    }

    public final List<Node<T>> querySquare(Node<T> center, double radius)
    {
        double[] first = new double[]{center.x - radius, center.y - radius};
        double[] second = new double[]{center.x + radius, center.y + radius};
        return quadTree.query(first, second);
    }

    public final List<Node<T>> queryCircle(Node<T> center, double radius)
    {
        val squareList = querySquare(center, radius);

        val result = new ArrayList<Node<T>>();
        val radiusSquared = radius * radius;
        for (Node<T> node : squareList) {
            if (node.inRadius(center, radiusSquared)) result.add(node);
        }
        return result;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c)
    {
        boolean changed = false;
        for (Object o : c) changed |= this.remove(o);
        return changed;
    }

    @Override
    public void clear()
    {
        for (Node<T> node : this) quadTree.remove(node, node.x, node.y);
        this.getBackingCollection().clear();
    }

    @Override
    public int size()
    {
        return getBackingCollection().size();
    }

    @Override
    public boolean isEmpty()
    {
        return getBackingCollection().isEmpty();
    }

    @Override
    public boolean contains(Object o)
    {
        return getBackingCollection().contains(o);
    }

    @NotNull
    @Override
    public Iterator<Node<T>> iterator()
    {
        return new Iterator<>()
        {
            private final Iterator<Node<T>> iter = getBackingCollection().iterator();
            private Node<T> current;

            @Override
            public boolean hasNext()
            {
                return iter.hasNext();
            }

            @Override
            public Node<T> next()
            {
                current = iter.next();
                return current;
            }

            @Override
            public void remove()
            {
                quadTree.remove(current, current.x, current.y);
                iter.remove();
            }
        };
    }

    @NotNull
    @Override
    public Object @NotNull [] toArray()
    {
        return getBackingCollection().toArray();
    }

    @NotNull
    @Override
    public <T1> T1 @NotNull [] toArray(T1 @NotNull [] a)
    {
        return getBackingCollection().toArray(a);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c)
    {
        return this.getBackingCollection().containsAll(c);
    }

    public record Node<T>(double x, double y, T element)
    {
        public boolean inRadius(Node<T> center, double squaredRadius)
        {
            return distanceSquared(center) <= squaredRadius;
        }

        public double distanceSquared(Node<T> other)
        {
            return MathUtil.squareSum(this.x - other.x, this.y - other.y);
        }

        public double distance(Node<T> other)
        {
            return MathUtil.fastHypot(this.x - other.x, this.y - other.y);
        }
    }
}
