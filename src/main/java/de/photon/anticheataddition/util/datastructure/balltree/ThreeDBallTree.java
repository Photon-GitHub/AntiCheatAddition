package de.photon.anticheataddition.util.datastructure.balltree;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.util.datastructure.Pair;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

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

        BallTreeNode<T> node = new BallTreeNode<>();

        if (points.size() == 1) {
            // Base case: leaf node
            node.point = points.get(0);
            node.centroid = coordinates.get(0).clone();
            node.radius = 0;
            node.isLeaf = true;
            node.pointCount = 1;
        } else {
            // Compute centroid and radius
            Vector centroid = computeCentroid(coordinates);
            double radius = coordinates.stream().mapToDouble(centroid::distance).max().orElse(0);
            node.centroid = centroid;
            node.radius = radius;
            node.pointCount = points.size();
            node.isLeaf = false;

            // Partition the points into two clusters
            Pair<List<T>, List<Vector>> leftCluster;
            Pair<List<T>, List<Vector>> rightCluster;

            // Use a simple clustering method (e.g., farthest point clustering)
            Pair<Vector, Vector> centers = selectInitialCenters(coordinates);
            leftCluster = new Pair<>(new ArrayList<>(), new ArrayList<>());
            rightCluster = new Pair<>(new ArrayList<>(), new ArrayList<>());

            // Assign points to the nearest center
            for (int i = 0; i < points.size(); i++) {
                Vector coord = coordinates.get(i);
                T point = points.get(i);

                double distToCenter1 = coord.distance(centers.first());
                double distToCenter2 = coord.distance(centers.second());

                if (distToCenter1 <= distToCenter2) {
                    leftCluster.first().add(point);
                    leftCluster.second().add(coord);
                } else {
                    rightCluster.first().add(point);
                    rightCluster.second().add(coord);
                }
            }

            // Recursively build child nodes
            node.leftChild = buildTree(leftCluster.first(), leftCluster.second());
            node.rightChild = buildTree(rightCluster.first(), rightCluster.second());
        }

        return node;
    }

    private Pair<Vector, Vector> selectInitialCenters(List<Vector> coordinates)
    {
        // Select two farthest points as initial centers
        int n = coordinates.size();
        int idx1 = 0;
        int idx2 = 1;
        double maxDistance = coordinates.get(0).distanceSquared(coordinates.get(1));

        for (int i = 0; i < n; i++) {
            Vector coord1 = coordinates.get(i);
            for (int j = i + 1; j < n; j++) {
                Vector coord2 = coordinates.get(j);
                double dist = coord1.distanceSquared(coord2);
                if (dist > maxDistance) {
                    maxDistance = dist;
                    idx1 = i;
                    idx2 = j;
                }
            }
        }

        return new Pair<>(coordinates.get(idx1), coordinates.get(idx2));
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
            return;
        }

        BallTreeNode<T> node = root;
        Deque<BallTreeNode<T>> pathStack = new ArrayDeque<>();

        while (true) {
            pathStack.push(node);

            // Update centroid and point count
            node.centroid = node.centroid.clone().multiply(node.pointCount).add(coordinate).multiply(1.0 / (node.pointCount + 1));
            node.pointCount += 1;

            // Update radius
            double distToNewPoint = node.centroid.distance(coordinate);
            node.radius = Math.max(node.radius, distToNewPoint);

            if (node.isLeaf) {
                // If node is a leaf, split it
                T existingPoint = node.point;
                Vector existingCoordinate = node.centroid.clone();

                // Create new child nodes
                node.point = null;
                node.isLeaf = false;

                // Use the same clustering logic as in buildTree
                Pair<Vector, Vector> centers = new Pair<>(existingCoordinate, coordinate);

                // Assign points to the nearest center
                double distToExisting = coordinate.distance(existingCoordinate);
                double distToNew = coordinate.distance(coordinate);

                if (distToExisting <= distToNew) {
                    node.leftChild = new BallTreeNode<>(existingPoint, existingCoordinate, 0, true, 1, null, null);
                    node.rightChild = new BallTreeNode<>(point, coordinate.clone(), 0, true, 1, null, null);
                } else {
                    node.leftChild = new BallTreeNode<>(point, coordinate.clone(), 0, true, 1, null, null);
                    node.rightChild = new BallTreeNode<>(existingPoint, existingCoordinate, 0, true, 1, null, null);
                }

                // Update parent node's radius and centroid
                node.centroid = computeCentroid(List.of(existingCoordinate, coordinate));
                node.radius = Math.max(
                        node.leftChild.centroid.distance(node.centroid) + node.leftChild.radius,
                        node.rightChild.centroid.distance(node.centroid) + node.rightChild.radius
                                      );
                node.pointCount = 2;

                break;
            } else {
                // Decide which child to proceed to
                double distToLeft = node.leftChild.centroid.distance(coordinate);
                double distToRight = node.rightChild.centroid.distance(coordinate);

                if (distToLeft <= distToRight) {
                    node = node.leftChild;
                } else {
                    node = node.rightChild;
                }
            }
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
        if (root == null) return;

        BallTreeNode<T> node = root;
        BallTreeNode<T> parent = null;
        boolean isLeftChild = false;

        Deque<TraversalState<T>> pathStack = new ArrayDeque<>();

        while (node != null) {
            pathStack.push(new TraversalState<>(node, parent, isLeftChild));

            if (node.isLeaf) {
                if (node.point.equals(point)) {
                    // Found the point to remove
                    break;
                } else {
                    // Point not found
                    return;
                }
            }

            parent = node;

            // Decide which child to proceed to
            double distToLeft = node.leftChild == null ? Double.POSITIVE_INFINITY : node.leftChild.centroid.distance(coordinate);
            double distToRight = node.rightChild == null ? Double.POSITIVE_INFINITY : node.rightChild.centroid.distance(coordinate);

            if (distToLeft <= distToRight) {
                node = node.leftChild;
                isLeftChild = true;
            } else {
                node = node.rightChild;
                isLeftChild = false;
            }
        }

        // If point not found
        if (node == null || !node.point.equals(point)) {
            return;
        }

        // Remove the point
        if (parent == null) {
            // Removing the root node
            root = null;
        } else {
            if (isLeftChild) {
                parent.leftChild = null;
            } else {
                parent.rightChild = null;
            }
        }

        // Update centroids and radii up the path
        while (!pathStack.isEmpty()) {
            TraversalState<T> state = pathStack.pop();
            BallTreeNode<T> currentNode = state.node;

            currentNode.pointCount -= 1;

            if (currentNode.pointCount == 0) {
                // Remove this node
                if (state.parent != null) {
                    if (state.isLeftChild) {
                        state.parent.leftChild = null;
                    } else {
                        state.parent.rightChild = null;
                    }
                } else {
                    root = null;
                }
                continue;
            }

            // Recompute centroid and radius
            List<Vector> childCentroids = new ArrayList<>();
            List<Integer> childPointCounts = new ArrayList<>();

            if (currentNode.leftChild != null) {
                childCentroids.add(currentNode.leftChild.centroid.clone().multiply(currentNode.leftChild.pointCount));
                childPointCounts.add(currentNode.leftChild.pointCount);
            }

            if (currentNode.rightChild != null) {
                childCentroids.add(currentNode.rightChild.centroid.clone().multiply(currentNode.rightChild.pointCount));
                childPointCounts.add(currentNode.rightChild.pointCount);
            }

            if (!childCentroids.isEmpty()) {
                Vector newCentroid = new Vector(0, 0, 0);
                int totalPoints = 0;

                for (int i = 0; i < childCentroids.size(); i++) {
                    newCentroid.add(childCentroids.get(i));
                    totalPoints += childPointCounts.get(i);
                }

                newCentroid.multiply(1.0 / totalPoints);
                currentNode.centroid = newCentroid;
                currentNode.pointCount = totalPoints;

                // Update radius
                double maxRadius = 0;
                if (currentNode.leftChild != null) {
                    double leftRadius = currentNode.leftChild.centroid.distance(currentNode.centroid) + currentNode.leftChild.radius;
                    maxRadius = Math.max(maxRadius, leftRadius);
                }
                if (currentNode.rightChild != null) {
                    double rightRadius = currentNode.rightChild.centroid.distance(currentNode.centroid) + currentNode.rightChild.radius;
                    maxRadius = Math.max(maxRadius, rightRadius);
                }
                currentNode.radius = maxRadius;
            }
        }
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
