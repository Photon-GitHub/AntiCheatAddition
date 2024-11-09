package de.photon.anticheataddition.util.datastructure.balltree;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.ToDoubleFunction;

/**
 * A 3D Ball Tree implementation for efficient spatial partitioning, range queries, and dynamic updates.
 *
 * @param <T> The type of the objects stored in the tree.
 */
public class ThreeDBallTree<T>
{
    private BallTreeNode<T> root;

    /**
     * Constructs a 3D Ball Tree from the provided points and their corresponding coordinates.
     *
     * @param points      The list of points to be stored in the tree.
     * @param coordinates The list of 3D coordinates corresponding to the points.
     */
    public ThreeDBallTree(List<T> points, List<Vector> coordinates)
    {
        Preconditions.checkArgument(points.size() == coordinates.size(), "Each point must have one corresponding coordinate.");
        root = buildTree(points, coordinates);
    }

    /**
     * Builds the ball tree iteratively.
     *
     * @param points      The points to store in the tree.
     * @param coordinates The 3D coordinates of the points.
     *
     * @return The root node of the ball tree.
     */
    private BallTreeNode<T> buildTree(List<T> points, List<Vector> coordinates)
    {
        if (points.isEmpty()) {
            return null;
        }

        Deque<NodeBuildTask<T>> stack = new ArrayDeque<>();
        BallTreeNode<T> rootNode = new BallTreeNode<>();
        stack.push(new NodeBuildTask<>(points, coordinates, rootNode));

        while (!stack.isEmpty()) {
            NodeBuildTask<T> task = stack.pop();
            List<T> pts = task.points;
            List<Vector> ptCoords = task.coordinates;
            BallTreeNode<T> node = task.node;

            Vector centroid = computeCentroid(ptCoords);
            node.radius = ptCoords.stream().mapToDouble(centroid::distance).max().orElse(0);

            node.centroid = centroid;
            node.pointCount = pts.size();

            if (pts.size() == 1) {
                node.point = pts.get(0);
                node.isLeaf = true;
            } else {
                Axis axis = chooseSplitAxis(ptCoords);
                List<Integer> indices = new ArrayList<>();
                for (int i = 0; i < ptCoords.size(); i++) {
                    indices.add(i);
                }
                indices.sort(Comparator.comparingDouble(i -> getAxisValue(ptCoords.get(i), axis)));

                int median = indices.size() / 2;
                List<Integer> leftIndices = indices.subList(0, median);
                List<Integer> rightIndices = indices.subList(median, indices.size());

                List<Vector> leftCoords = leftIndices.stream().map(ptCoords::get).toList();
                List<Vector> rightCoords = rightIndices.stream().map(ptCoords::get).toList();

                List<T> leftPoints = leftIndices.stream().map(pts::get).toList();
                List<T> rightPoints = rightIndices.stream().map(pts::get).toList();

                node.leftChild = new BallTreeNode<>();
                node.rightChild = new BallTreeNode<>();

                stack.push(new NodeBuildTask<>(rightPoints, rightCoords, node.rightChild));
                stack.push(new NodeBuildTask<>(leftPoints, leftCoords, node.leftChild));
            }
        }

        return rootNode;
    }

    private enum Axis
    {
        X, Y, Z
    }

    /**
     * Chooses the axis with the largest range for splitting points.
     *
     * @param coordinates The list of coordinates.
     *
     * @return The axis to split on.
     */
    private Axis chooseSplitAxis(List<Vector> coordinates)
    {
        double rangeX = computeRange(coordinates, Vector::getX);
        double rangeY = computeRange(coordinates, Vector::getY);
        double rangeZ = computeRange(coordinates, Vector::getZ);

        if (rangeX >= rangeY && rangeX >= rangeZ) return Axis.X;
        else if (rangeY >= rangeZ) return Axis.Y;
        else return Axis.Z;
    }

    private double computeRange(List<Vector> vectors, ToDoubleFunction<Vector> extractor)
    {
        DoubleSummaryStatistics stats = vectors.stream().mapToDouble(extractor).summaryStatistics();
        return stats.getMax() - stats.getMin();
    }

    private double getAxisValue(Vector point, Axis axis)
    {
        return switch (axis) {
            case X -> point.getX();
            case Y -> point.getY();
            case Z -> point.getZ();
        };
    }

    /**
     * Performs a range search to find all points within a given radius from a target point.
     *
     * @param target The target point for the range search.
     * @param radius The search radius.
     *
     * @return A list of points within the specified radius.
     */
    public List<T> rangeSearch(Vector target, double radius)
    {
        if (root == null) return List.of();

        final List<T> result = new ArrayList<>();
        final Deque<BallTreeNode<T>> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            final BallTreeNode<T> node = stack.pop();

            // Ensure centroid is not null
            if (node.centroid == null) continue;

            final double dist = node.centroid.distance(target);

            if (dist - node.radius > radius) {
                continue; // Node is outside the search radius
            }

            if (node.isLeaf) {
                if (node.point != null && node.centroid.distance(target) <= radius) {
                    result.add(node.point);
                }
                continue;
            }

            if (node.leftChild != null) stack.push(node.leftChild);
            if (node.rightChild != null) stack.push(node.rightChild);
        }

        return result;
    }

    /**
     * Computes the centroid of a list of 3D vectors.
     *
     * @param vectors The list of vectors.
     *
     * @return The centroid vector.
     */
    private static Vector computeCentroid(List<Vector> vectors)
    {
        final Vector sum = new Vector();
        for (Vector vector : vectors) sum.add(vector);
        return sum.multiply(1.0 / vectors.size());
    }

    /**
     * Inserts a new point into the Ball Tree iteratively.
     *
     * @param point      The point to insert.
     * @param coordinate The coordinate of the point.
     */
    public void insert(T point, Vector coordinate)
    {
        // Create a new root node if tree is empty
        if (root == null) {
            root = new BallTreeNode<>(point, coordinate.clone(), 0, true, 1, null, null);
        } else {
            insertIterative(point, coordinate);
        }
    }

    private void insertIterative(T point, Vector coordinate)
    {
        BallTreeNode<T> node = root;
        final Deque<BallTreeNode<T>> pathStack = new ArrayDeque<>();

        while (true) {
            pathStack.push(node);

            // Update centroid and point count
            double totalPoints = node.pointCount + 1.0;
            Vector newCentroid = node.centroid.clone().multiply(node.pointCount).add(coordinate).multiply(1.0 / totalPoints);
            node.pointCount += 1;

            // Update radius
            double distToNewPoint = newCentroid.distance(coordinate);
            double distCentroidShift = newCentroid.distance(node.centroid);
            node.radius = Math.max(node.radius + distCentroidShift, distToNewPoint);
            node.centroid = newCentroid;

            if (node.isLeaf) {
                // Break to split the leaf node
                break;
            } else {
                // Decide which child to proceed
                double distToLeft = node.leftChild.centroid.distance(coordinate);
                double distToRight = node.rightChild.centroid.distance(coordinate);

                node = distToLeft <= distToRight ? node.leftChild : node.rightChild;
            }
        }

        // Split the leaf node
        BallTreeNode<T> leafNode = node;
        T existingPoint = leafNode.point;
        Vector existingCoordinate = leafNode.centroid.clone();

        leafNode.isLeaf = false;
        leafNode.point = null;

        Axis splitAxis = chooseSplitAxis(List.of(existingCoordinate, coordinate));

        if (getAxisValue(existingCoordinate, splitAxis) < getAxisValue(coordinate, splitAxis)) {
            // Existing point to left child
            leafNode.leftChild = new BallTreeNode<>(existingPoint, existingCoordinate, 0, true, 1, null, null);
            leafNode.rightChild = new BallTreeNode<>(point, coordinate.clone(), 0, true, 1, null, null);
        } else {
            leafNode.leftChild = new BallTreeNode<>(point, coordinate.clone(), 0, true, 1, null, null);
            leafNode.rightChild = new BallTreeNode<>(existingPoint, existingCoordinate, 0, true, 1, null, null);
        }

        // Update the radii and centroids up the path
        pathStack.pop(); // Already updated leaf node

        while (!pathStack.isEmpty()) {
            BallTreeNode<T> currentNode = pathStack.pop();

            // Update centroid
            currentNode.centroid = currentNode.leftChild.centroid.clone().multiply(currentNode.leftChild.pointCount)
                                                                 .add(currentNode.rightChild.centroid.clone().multiply(currentNode.rightChild.pointCount))
                                                                 .multiply(1.0 / currentNode.pointCount);

            // Update radius
            double leftRadius = currentNode.leftChild.centroid.distance(currentNode.centroid) + currentNode.leftChild.radius;
            double rightRadius = currentNode.rightChild.centroid.distance(currentNode.centroid) + currentNode.rightChild.radius;
            currentNode.radius = Math.max(leftRadius, rightRadius);
        }
    }

    /**
     * Removes a point from the Ball Tree iteratively.
     *
     * @param point      The point to remove.
     * @param coordinate The coordinate of the point.
     */
    public void remove(T point, Vector coordinate)
    {
        if (root != null) {
            removeIterative(point, coordinate);
        }
    }

    private void removeIterative(T point, Vector coordinate)
    {
        BallTreeNode<T> node = root;
        BallTreeNode<T> parent = null;
        boolean isLeftChild = false;

        Deque<TraversalState<T>> pathStack = new ArrayDeque<>();

        boolean found = false;

        while (node != null) {
            pathStack.push(new TraversalState<>(node, parent, isLeftChild));

            if (node.isLeaf) {
                if (node.point.equals(point)) {
                    found = true;
                    break;
                }
                // Point not found
                break;
            }

            parent = node;

            // Decide which child to proceed
            double distToLeft = node.leftChild.centroid.distance(coordinate);
            double distToRight = node.rightChild.centroid.distance(coordinate);

            if (distToLeft <= distToRight) {
                node = node.leftChild;
                isLeftChild = true;
            } else {
                node = node.rightChild;
                isLeftChild = false;
            }
        }

        if (!found) {
            // Point not found, no need to update pointCounts
            return;
        }

        // Now, decrement pointCounts along the path
        for (TraversalState<T> state : pathStack) {
            state.node.pointCount -= 1;
        }

        // Remove the node
        TraversalState<T> leafState = pathStack.peek();
        if (leafState.parent == null) {
            // Removing the root node
            root = null;
        } else {
            if (leafState.isLeftChild) {
                leafState.parent.leftChild = null;
            } else {
                leafState.parent.rightChild = null;
            }
        }

        // Update the centroids and radii up the path
        while (!pathStack.isEmpty()) {
            TraversalState<T> state = pathStack.pop();
            BallTreeNode<T> currentNode = state.node;
            BallTreeNode<T> parentNode = state.parent;
            boolean isLeft = state.isLeftChild;

            if (currentNode.leftChild == null && currentNode.rightChild == null) {
                // Node has no children, remove it from its parent
                if (parentNode != null) {
                    if (isLeft) {
                        parentNode.leftChild = null;
                    } else {
                        parentNode.rightChild = null;
                    }
                } else {
                    // Tree is now empty
                    root = null;
                }
            } else if (currentNode.leftChild == null) {
                // Only right child exists, promote right child
                BallTreeNode<T> promotedChild = currentNode.rightChild;
                copyNodeData(currentNode, promotedChild);
            } else if (currentNode.rightChild == null) {
                // Only left child exists, promote left child
                BallTreeNode<T> promotedChild = currentNode.leftChild;
                copyNodeData(currentNode, promotedChild);
            } else {
                // Both children exist, update centroid and radius
                currentNode.centroid = currentNode.leftChild.centroid.clone().multiply(currentNode.leftChild.pointCount)
                                                                     .add(currentNode.rightChild.centroid.clone().multiply(currentNode.rightChild.pointCount))
                                                                     .multiply(1.0 / currentNode.pointCount);

                double leftRadius = currentNode.leftChild.centroid.distance(currentNode.centroid) + currentNode.leftChild.radius;
                double rightRadius = currentNode.rightChild.centroid.distance(currentNode.centroid) + currentNode.rightChild.radius;
                currentNode.radius = Math.max(leftRadius, rightRadius);
            }
        }
    }

    private void copyNodeData(BallTreeNode<T> destination, BallTreeNode<T> source)
    {
        destination.point = source.point;
        destination.centroid = source.centroid;
        destination.radius = source.radius;
        destination.isLeaf = source.isLeaf;
        destination.pointCount = source.pointCount;
        destination.leftChild = source.leftChild;
        destination.rightChild = source.rightChild;
    }

    /**
     * Represents a node in the Ball Tree.
     *
     * @param <T> The type of the object stored in the node.
     */
    @AllArgsConstructor
    @NoArgsConstructor
    private static class BallTreeNode<T>
    {
        private T point;
        private Vector centroid;
        private double radius;
        private boolean isLeaf;
        private int pointCount;
        private BallTreeNode<T> leftChild;
        private BallTreeNode<T> rightChild;
    }

    /**
     * Represents a task for building a node in the Ball Tree.
     *
     * @param <T> The type of the points in the task.
     */
    private record NodeBuildTask<T>(List<T> points, List<Vector> coordinates, BallTreeNode<T> node) {}

    /**
     * Represents the traversal state during insertion or removal.
     *
     * @param <T> The type of the objects in the tree.
     */
    private record TraversalState<T>(BallTreeNode<T> node, BallTreeNode<T> parent, boolean isLeftChild) {}
}
