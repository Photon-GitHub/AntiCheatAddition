package de.photon.aacadditionpro.util.datastructure.kdtree;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.val;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

public class FixedSize2DTree<T>
{
    private static final Comparator<TreeNode<?>> X_COMPARATOR = Comparator.comparingDouble(TreeNode::getX);
    private static final Comparator<TreeNode<?>> Y_COMPARATOR = Comparator.comparingDouble(TreeNode::getY);

    private final double[] xArray;
    private final TreeNode<T>[] xNodeArray;
    private final double[] yArray;
    private final TreeNode<T>[] yNodeArray;

    public FixedSize2DTree(Collection<TreeNode<T>> nodes)
    {
        val size = nodes.size();
        xArray = new double[size];
        xNodeArray = new TreeNode[size];
        yArray = new double[size];
        yNodeArray = new TreeNode[size];

        int index = 0;
        for (TreeNode<T> node : nodes) {
            xNodeArray[index] = node;
            xArray[index] = node.x;
            yNodeArray[index] = node;
            yArray[index] = node.y;
        }

        Arrays.sort(xArray);
        Arrays.sort(xNodeArray, X_COMPARATOR);
        Arrays.sort(yArray);
        Arrays.sort(yNodeArray, Y_COMPARATOR);
    }


    public Set<TreeNode<T>> getSquare(double x, double y, double radius)
    {
        var xmin = Arrays.binarySearch(xArray, x - radius);
        var xmax = Arrays.binarySearch(xArray, x + radius);

        var ymin = Arrays.binarySearch(yArray, y - radius);
        var ymax = Arrays.binarySearch(yArray, y + radius);

    }


    @Value
    public static class TreeNode<T>
    {
        double x;
        double y;
        @EqualsAndHashCode.Exclude T value;
    }
}
