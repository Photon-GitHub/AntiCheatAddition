package de.photon.aacadditionpro.util.datastructure.kdtree;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.val;

import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * A simple implementation of a kd-tree of 2 dimensions with 2 {@link TreeMap}s.
 * This is not an optimal implementation.
 */
public class Simple2dTree<T>
{
    private final Random random = new Random();
    private final NavigableMap<Double, TreeNode<T>> xTree = new TreeMap<>();
    private final NavigableMap<Double, TreeNode<T>> yTree = new TreeMap<>();

    public boolean isEmpty()
    {
        return xTree.isEmpty();
    }

    public void add(double x, double y, T value)
    {
        val node = new TreeNode<>(x, y, value);
        xTree.put(x, node);
        yTree.put(y, node);
    }

    public void add(double x, double y, double offset, int tries, T value)
    {
        boolean containsX = true;
        boolean containsY = true;
        for (int i = 0; i < tries && (containsX || containsY); i++) {
            containsX = xTree.containsKey(x);
            containsY = yTree.containsKey(y);
            if (containsX) x = x + random.nextDouble() * offset;
            if (containsY) y = y + random.nextDouble() * offset;
        }

        // Could not insert.
        if (containsX || containsY) return;
        val node = new TreeNode<T>(x, y, value);
        xTree.put(x, node);
        yTree.put(y, node);
    }

    public void remove(TreeNode<T> node)
    {
        xTree.remove(node.x);
        yTree.remove(node.y);
    }

    public void removeAll(Iterable<TreeNode<T>> nodes)
    {
        for (TreeNode<T> node : nodes) remove(node);
    }

    public TreeNode<T> getFirstX()
    {
        return xTree.firstEntry().getValue();
    }

    public TreeNode<T> getFirstY()
    {
        return yTree.firstEntry().getValue();
    }

    public Set<TreeNode<T>> getSquare(TreeNode<T> node, double radius)
    {
        return getSquare(node.x, node.y, radius);
    }

    public Set<TreeNode<T>> getSquare(double x, double y, double radius)
    {
        val xCol = xTree.subMap(x - radius, x + radius).values();
        return yTree.subMap(y - radius, y + radius).values().stream().filter(xCol::contains).collect(Collectors.toUnmodifiableSet());
    }

    @Value
    public static class TreeNode<T>
    {
        double x;
        double y;
        @EqualsAndHashCode.Exclude T value;
    }
}
