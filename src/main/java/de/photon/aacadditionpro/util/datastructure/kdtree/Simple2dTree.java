package de.photon.aacadditionpro.util.datastructure.kdtree;

import java.util.Map;
import java.util.TreeMap;

public class Simple2dTree<T>
{
    private final TreeMap<Double, T> xTree = new TreeMap<>();
    private final TreeMap<Double, T> yTree = new TreeMap<>();

    public void add(double x, double y, T value)
    {
        xTree.put(x, value);
        yTree.put(y, value);
    }

    public void remove(double x, double y)
    {
        xTree.remove(x);
        yTree.remove(y);
    }

    public void remove(Map.Entry<Double, T> entry)
    {
        xTree.remove(entry.getKey(), entry.getValue());
        yTree.remove(entry.getKey(), entry.getValue());
    }
}
