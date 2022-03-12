package de.photon.anticheataddition.util.datastructure.kdtree;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;

/**
 * This is an implementation with a backing {@link ArrayDeque}.
 * Therefore, getAny() is a very fast operation, but contains and remove are O(n)
 */
public class QuadTreeQueue<T> extends QuadTreeCollection<T>
{
    private final Queue<Node<T>> nodes = new ArrayDeque<>();

    @Override
    protected Collection<Node<T>> getBackingCollection()
    {
        return nodes;
    }

    @Override
    public Node<T> getAny()
    {
        return nodes.peek();
    }

    @Override
    public Node<T> removeAny()
    {
        var any = nodes.poll();
        if (any != null) quadTree.remove(any, any.x, any.y);
        return any;
    }
}
