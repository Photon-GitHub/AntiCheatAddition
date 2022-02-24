package de.photon.aacadditionpro.util.datastructure.kdtree;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.val;
import org.danilopianini.util.FlexibleQuadTree;
import org.danilopianini.util.SpatialIndex;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class QuadTreeIteration<T> implements Set<QuadTreeIteration.Node<T>>
{
    private final SpatialIndex<Node<T>> quadTree = new FlexibleQuadTree<>();
    private final Set<Node<T>> nodes = new HashSet<>();

    @Override
    public boolean add(Node<T> node)
    {
        this.add(node.x, node.y, node.element);
        return true;
    }

    public void add(double x, double y, T element)
    {
        val node = new Node<>(x, y, element);
        quadTree.insert(node, x, y);
        nodes.add(node);
    }

    public Node<T> getAny()
    {
        var iter = nodes.iterator();
        return iter.hasNext() ? iter.next() : null;
    }

    @Override
    public boolean remove(Object o)
    {
        return o instanceof Node<?> && this.remove((Node<T>) o);
    }

    public boolean remove(Node<T> node)
    {
        quadTree.remove(node, node.x, node.y);
        return nodes.remove(node);
    }

    public List<Node<T>> querySquare(Node<T> center, double radius)
    {
        double[] first = new double[]{center.x - radius, center.y - radius};
        double[] second = new double[]{center.x + radius, center.y + radius};
        return quadTree.query(first, second);
    }

    public List<Node<T>> queryCircle(Node<T> center, double radius)
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
    public void clear()
    {
        for (Node<T> node : this) quadTree.remove(node);
        this.nodes.clear();
    }

    @Override
    public int size()
    {
        return nodes.size();
    }

    @Override
    public boolean isEmpty()
    {
        return nodes.isEmpty();
    }

    @Override
    public boolean contains(Object o)
    {
        return nodes.contains(o);
    }

    @NotNull
    @Override
    public Iterator<Node<T>> iterator()
    {
        return new Iterator<>()
        {
            private final Iterator<Node<T>> iter = nodes.iterator();
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
    public Object[] toArray()
    {
        return nodes.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a)
    {
        return nodes.toArray(a);
    }


    @Override
    public boolean containsAll(@NotNull Collection<?> c)
    {
        return this.nodes.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends Node<T>> c)
    {
        for (Node<T> tNode : c) this.add(tNode);
        return true;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c)
    {
        boolean changed = false;
        for (Object o : c) changed |= this.remove(o);
        return changed;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c)
    {
        boolean changed = false;
        for (Node<T> n : this) {
            if (!c.contains(n)) {
                changed = true;
                remove(n);
            }
        }
        return changed;
    }

    @Value
    public static class Node<T>
    {
        double x;
        double y;
        @EqualsAndHashCode.Exclude T element;

        public boolean inRadius(Node<T> center, double squaredRadius)
        {
            double cX = this.x - center.x;
            double cY = this.y - center.y;
            return (cX * cX + cY * cY) <= squaredRadius;
        }
    }
}
