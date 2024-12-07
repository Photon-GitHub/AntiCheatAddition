package de.photon.anticheataddition.util.datastructure.balltree;

import com.google.common.base.Preconditions;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A 3D Ball Tree implementation for efficient spatial partitioning, range queries, and dynamic updates.
 * <p>
 * The Ball Tree is a binary tree-based data structure that organizes points in a 3D space.
 * Each node in the tree represents a sphere (or "ball") defined by a center point and a radius,
 * which either contains points (for leaf nodes) or references to child nodes (for internal nodes).
 * This data structure supports operations such as insertion, range search, and point removal efficiently.
 *
 * @param <T> The type of the data associated with each point in the tree.
 */
public class ThreeDBallTree<T> extends AbstractCollection<T> implements Collection<T>
{
    public static final int EXPECTED_NODES = 128;
    private static final int MAX_LEAF_SIZE = 4;

    private Node<T> root;
    private int size;

    /**
     * Constructs an empty 3D Ball Tree.
     */
    public ThreeDBallTree()
    {
        root = new Node<>(new ArrayList<>());
        size = 0;
    }

    /**
     * Constructs a 3D Ball Tree from a collection of points.
     *
     * @param points The initial collection of points to populate the tree.
     */
    public ThreeDBallTree(Collection<BallTreePoint<T>> points)
    {
        buildTreeIteratively(points);
        size = points.size();
    }

    /**
     * Builds the tree structure iteratively using a collection of points.
     * <p>
     * This method constructs the tree by creating a root node containing all points
     * and repeatedly splitting nodes until all nodes satisfy the maximum leaf size constraint.
     *
     * @param points The collection of points to add to the tree.
     */
    private void buildTreeIteratively(Collection<BallTreePoint<T>> points)
    {
        // Create the root node and add all the points (most likely larger than the max leaf size.
        root = new Node<>(new ArrayList<>(points));

        // Create the node queue.
        final Queue<Node<T>> queue = new ArrayDeque<>();
        queue.add(root);

        // Start the splitting process
        while (!queue.isEmpty()) {
            Node<T> node = queue.poll();

            // If a node is overfull, split it and add the new children to the queue.
            if (node.points.size() > MAX_LEAF_SIZE) {
                node.splitNode();
                queue.add(node.leftChild);
                queue.add(node.rightChild);
            }
        }
    }

    /**
     * Recomputes the center and radius of nodes along a given path.
     * <p>
     * This is used to update the tree structure after modifications such as insertion or removal.
     *
     * @param path A stack representing the path of nodes to update, starting from a leaf and ending at the root.
     */
    private void recomputeCenterAndRadii(Deque<Node<T>> path)
    {
        Node<T> node;
        while (!path.isEmpty()) {
            node = path.pop();
            node.computeCenterAndRadius();
        }
    }

    /**
     * Inserts a new {@link BallTreePoint} into the tree.
     * <p>
     * The insertion process finds the appropriate leaf node for the new point,
     * adds the point to the node, and splits the node if necessary.
     * After insertion, parent node values are updated to reflect the changes.
     *
     * @param point The point to insert into the tree.
     */
    public void insert(BallTreePoint<T> point)
    {
        // Create the path stack for later recalculation of the parent nodes.
        final Deque<Node<T>> path = new ArrayDeque<>();
        Node<T> current = root;

        // Search for a leaf node
        while (!current.isLeaf()) {
            // Add the current node to the path
            path.push(current);

            // Go to the child which has the nearest distance to the point to add
            double distToLeft = distanceSquaredToNode(point, current.leftChild);
            double distToRight = distanceSquaredToNode(point, current.rightChild);
            current = distToLeft < distToRight ? current.leftChild : current.rightChild;
        }

        // Leaf node:
        // Add the point to the point list, increase the tree size and recompute the current node.
        current.points.add(point);
        ++this.size;
        current.computeCenterAndRadius();

        // If needed, split the node
        if (current.points.size() > MAX_LEAF_SIZE) current.splitNode();

        // Update centers and radii for all parent nodes that have been affected by this insertion.
        recomputeCenterAndRadii(path);
    }

    /**
     * Retrieves a {@link BallTreePoint} with the specified coordinates from the tree.
     * <p>
     * The method performs a search to locate a point with the exact coordinates (x, y, z).
     *
     * @param x The x-coordinate of the point.
     * @param y The y-coordinate of the point.
     * @param z The z-coordinate of the point.
     *
     * @return The point if found, or {@code null} if no such point exists.
     */
    public BallTreePoint<T> get(double x, double y, double z)
    {
        // Create the stack for the search and add the root node.
        final Deque<Node<T>> stack = new ArrayDeque<>();
        stack.push(root);

        // Start the search
        while (!stack.isEmpty()) {
            Node<T> node = stack.pop();

            // Check if the current node includes the coordinates to search for, and omit the node if not.
            if (distanceBetweenPoints(x, y, z, node.centerX, node.centerY, node.centerZ) > node.radius) continue;

            if (node.isLeaf()) {
                if (node.points != null) {
                    for (BallTreePoint<T> p : node.points) {
                        if (p.x() == x && p.y() == y && p.z() == z) {
                            return p;
                        }
                    }
                }
            } else {
                stack.push(node.leftChild);
                stack.push(node.rightChild);
            }
        }

        return null;
    }

    @Override
    public boolean remove(Object o)
    {
        // Use the remove method below.
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> points)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes all points in the specified collection from the tree.
     * <p>
     * Iterates over the collection and removes each point individually.
     *
     * @param points A collection of points to remove.
     *
     * @return {@code true} if any points were removed, {@code false} otherwise.
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean removePoints(Collection<BallTreePoint<T>> points)
    {
        Preconditions.checkNotNull(points, "Points to remove must be non-null.");
        boolean modified = false;

        for (BallTreePoint<T> p : points) {
            modified |= remove(p);
        }
        return modified;
    }

    /**
     * Removes a specific point from the tree.
     * <p>
     * Searches for the specified point in the tree, removes it if found, and updates parent nodes.
     *
     * @param point The {@link BallTreePoint} to remove from the tree.
     *
     * @return {@code true} if the point was successfully removed, {@code false} otherwise.
     */
    public boolean remove(BallTreePoint<T> point)
    {
        // Create the stack and path list for the removal
        final Deque<Node<T>> stack = new ArrayDeque<>();
        final Deque<Node<T>> path = new ArrayDeque<>();

        // Start with the root node
        stack.push(root);

        // Go through the stack
        while (!stack.isEmpty()) {
            // Get the first element and push it as a part of the current path.
            Node<T> node = stack.pop();

            // Check if the node is in the radius.
            if (distanceBetweenPoints(point.x(), point.y(), point.z(), node.centerX, node.centerY, node.centerZ) > node.radius) continue;

            // Add this node as a part of the path.
            path.push(node);

            // We have found a leaf.
            if (node.isLeaf()) {
                // Can we remove the point from that child?
                if (node.points != null && node.points.remove(point)) {
                    // If yes, remove the point.
                    --this.size;
                    // Recompute the parent nodes after removal.
                    recomputeCenterAndRadii(path);
                    return true;
                } else {
                    // We cannot remove the child here -> This is not the correct node.
                    path.pop();
                }
            } else {
                // Not a leaf node -> Add both successors.
                stack.push(node.leftChild);
                stack.push(node.rightChild);
            }
        }

        return false;
    }

    /**
     * Checks whether a specific point exists in the tree.
     *
     * @param point The point to check for existence.
     *
     * @return {@code true} if the point exists in the tree, {@code false} otherwise.
     */
    public boolean contains(BallTreePoint<T> point)
    {
        return get(point.x(), point.y(), point.z()) != null;
    }

    /**
     * Performs a range search to find all {@link BallTreePoint}s within a specified radius from a given point.
     * <p>
     * Searches the tree for points that lie within the radius from the specified target point.
     *
     * @param point  The target point for the range search.
     * @param radius The radius within which to search for points.
     *
     * @return A set of points within the specified radius.
     */
    public Set<BallTreePoint<T>> rangeSearch(BallTreePoint<T> point, double radius)
    {
        return rangeSearch(point.x(), point.y(), point.z(), radius);
    }

    /**
     * Performs a range search to find all points within a specified radius from a given location.
     * <p>
     * This method is specifically designed to work with {@link Location}s.
     *
     * @param location The location representing the center of the search radius.
     * @param radius   The radius within which to search for points.
     *
     * @return A set of points within the specified radius.
     */
    public Set<BallTreePoint<T>> rangeSearch(Location location, double radius)
    {
        return rangeSearch(location.getX(), location.getY(), location.getZ(), radius);
    }

    /**
     * Performs a range search to find all {@link BallTreePoint}s within a specified radius from a given coordinate.
     *
     * @param x      The x-coordinate of the search center.
     * @param y      The y-coordinate of the search center.
     * @param z      The z-coordinate of the search center.
     * @param radius The radius within which to search for points.
     *
     * @return A set of points within the specified radius.
     */
    public Set<BallTreePoint<T>> rangeSearch(double x, double y, double z, double radius)
    {
        // Create the result set, the stack and push the root node.
        final Set<BallTreePoint<T>> result = new HashSet<>();
        final Deque<Node<T>> stack = new ArrayDeque<>(EXPECTED_NODES);
        stack.push(root);

        // Calculate the squared radius to avoid calculating roots.
        final double radiusSquared = radius * radius;

        while (!stack.isEmpty()) {
            final Node<T> node = stack.pop();

            // Check if the radius intersects with the current sphere.
            final double distSquaredToNode = distanceSquaredBetweenPoints(x, y, z, node.centerX, node.centerY, node.centerZ);
            final double maxDist = node.radius + radius;

            // If not, drop this sphere as it will not contain any results.
            if (distSquaredToNode > maxDist * maxDist) continue;

            // Leaf node
            if (node.isLeaf()) {
                if (node.points != null) {
                    for (BallTreePoint<T> p : node.points) {
                        if (distanceSquaredBetweenPoints(x, y, z, p.x(), p.y(), p.z()) <= radiusSquared) result.add(p);
                    }
                }
            } else {
                // Internal node, add the children to the stack.
                stack.push(node.leftChild);
                stack.push(node.rightChild);
            }
        }

        return result;
    }

    /**
     * Computes the squared distance from a {@link BallTreePoint} to a tree node.
     *
     * @param point The point for which to compute the distance.
     * @param node  The node to which the distance is computed.
     *
     * @return The squared distance from the point to the node.
     */
    private double distanceSquaredToNode(BallTreePoint<T> point, Node<T> node)
    {
        final double dx = point.x() - node.centerX;
        final double dy = point.y() - node.centerY;
        final double dz = point.z() - node.centerZ;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Computes the distance between two 3D points.
     *
     * @param x1 The x-coordinate of the first point.
     * @param y1 The y-coordinate of the first point.
     * @param z1 The z-coordinate of the first point.
     * @param x2 The x-coordinate of the second point.
     * @param y2 The y-coordinate of the second point.
     * @param z2 The z-coordinate of the second point.
     *
     * @return The Euclidean distance between the two points.
     */
    private static double distanceBetweenPoints(double x1, double y1, double z1, double x2, double y2, double z2)
    {
        return Math.sqrt(distanceSquaredBetweenPoints(x1, y1, z1, x2, y2, z2));
    }

    /**
     * Computes the squared distance between two 3D points.
     *
     * @param x1 The x-coordinate of the first point.
     * @param y1 The y-coordinate of the first point.
     * @param z1 The z-coordinate of the first point.
     * @param x2 The x-coordinate of the second point.
     * @param y2 The y-coordinate of the second point.
     * @param z2 The z-coordinate of the second point.
     *
     * @return The squared Euclidean distance between the two points.
     */
    private static double distanceSquaredBetweenPoints(double x1, double y1, double z1, double x2, double y2, double z2)
    {
        final double dx = x1 - x2;
        final double dy = y1 - y2;
        final double dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Retrieves an arbitrary point from the tree.
     * <p>
     * This method assumes that the tree contains at least one point and retrieves a point from a populated leaf.
     *
     * @return An arbitrary point from the tree.
     *
     * @throws NoSuchElementException If the tree is empty.
     */
    public BallTreePoint<T> getAny()
    {
        if (root == null) throw new NoSuchElementException();

        // We assume that for every internal node, both the left and the right child nodes have a populated leaf at some point.
        var node = root;
        while (!node.isLeaf()) {
            node = node.leftChild;
        }
        return node.points.get(0);
    }

    @Override
    public @NotNull Iterator<T> iterator()
    {
        return new Iterator<>()
        {
            private final Deque<Node<T>> stack = new ArrayDeque<>();
            private Iterator<BallTreePoint<T>> pointIterator = null;

            {
                if (root != null) {
                    stack.push(root);
                    advanceToNextLeaf();
                }
            }

            private void advanceToNextLeaf()
            {
                while (!stack.isEmpty()) {
                    Node<T> node = stack.pop();
                    if (node.isLeaf()) {
                        if (node.points != null && !node.points.isEmpty()) {
                            pointIterator = node.points.iterator();
                            return;
                        }
                    } else {
                        // Push in this order to first process the left child (correct ordering with stacks).
                        stack.push(node.rightChild);
                        stack.push(node.leftChild);
                    }
                }
                pointIterator = null;
            }

            @Override
            public boolean hasNext()
            {
                if (pointIterator != null && pointIterator.hasNext()) {
                    return true;
                } else {
                    advanceToNextLeaf();
                    return pointIterator != null && pointIterator.hasNext();
                }
            }

            @Override
            public T next()
            {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return pointIterator.next().data;
            }
        };
    }


    @Override
    public int size()
    {
        return size;
    }

    /**
     * Represents a 3D point with associated data in the ball tree.
     *
     * @param <T> The type of the data associated with the point.
     */
    public record BallTreePoint<T>(double x, double y, double z, T data)
    {
        /**
         * Computes the squared distance between this point and another point.
         *
         * @param other The other point to which the distance is computed.
         *
         * @return The squared distance between the two points.
         */
        public double distanceSquared(BallTreePoint<T> other)
        {
            final double dx = other.x - x;
            final double dy = other.y - y;
            final double dz = other.z - z;
            return dx * dx + dy * dy + dz * dz;
        }

        public BallTreePoint(Location location, T data)
        {
            this(location.getX(), location.getY(), location.getZ(), data);
        }
    }

    /**
     * Represents a node in the 3D Ball Tree.
     * <p>
     * A node can either be a leaf node (containing a list of points) or an internal node
     * (with references to left and right child nodes and a bounding sphere).
     *
     * @param <T> The type of the data associated with points in the tree.
     */
    private static class Node<T>
    {
        private double centerX;
        private double centerY;
        private double centerZ;

        private double radius;

        private Node<T> leftChild;
        private Node<T> rightChild;

        @Nullable private List<BallTreePoint<T>> points; // Only for leaf nodes

        public Node(@Nullable List<BallTreePoint<T>> points)
        {
            this.points = points;
            computeCenterAndRadius();
        }

        public boolean isLeaf()
        {
            return this.leftChild == null || this.rightChild == null;
        }

        private void computeCenterAndRadius()
        {
            // Check for an internal node.
            if (!this.isLeaf()) {
                // As we have an internal node, we cover both children in our hypersphere
                // -> Center is between the children, radius the maximum to each of them and their radius.
                centerX = (leftChild.centerX + rightChild.centerX) / 2;
                centerY = (leftChild.centerY + rightChild.centerY) / 2;
                centerZ = (leftChild.centerZ + rightChild.centerZ) / 2;

                final double distToLeft = this.distance(leftChild) + leftChild.radius;
                final double distToRight = this.distance(rightChild) + rightChild.radius;
                radius = Math.max(distToLeft, distToRight);
                return;
            }

            // Leaf node
            // Here, we use all the points in the leaf to calculate the center and the radius of the sphere.
            final double[] sum = new double[3];

            for (BallTreePoint<T> p : points) {
                sum[0] += p.x();
                sum[1] += p.y();
                sum[2] += p.z();
            }

            final int n = points.size();
            centerX = sum[0] / n;
            centerY = sum[1] / n;
            centerZ = sum[2] / n;

            final double maxDistanceSquared = points.stream().mapToDouble(p -> distanceSquaredBetweenPoints(p.x(), p.y(), p.z(), centerX, centerY, centerZ)).max().orElse(0);

            radius = Math.sqrt(maxDistanceSquared);
        }

        public void splitNode()
        {
            // Cannot split further, must have at least 2 points.
            if (points == null || points.size() <= 1) throw new IllegalStateException("To split a node, at least two points are required.");

            // Step 1: Choose the first point as the first seed.
            final BallTreePoint<T> seed1 = points.get(0);

            // Step 2: Find the point furthest from seed1 to be seed2
            BallTreePoint<T> seed2 = seed1;
            double maxDistSquared = 0;
            for (BallTreePoint<T> p : points) {
                double distSquared = seed1.distanceSquared(p);
                if (distSquared > maxDistSquared) {
                    maxDistSquared = distSquared;
                    seed2 = p;
                }
            }

            // Cannot split if the points are the same.
            if (seed1.equals(seed2)) return;

            // Step 3: Assign points to the nearest seed
            List<BallTreePoint<T>> leftPoints = new ArrayList<>();
            List<BallTreePoint<T>> rightPoints = new ArrayList<>();

            for (BallTreePoint<T> p : points) {
                final double distToSeed1 = p.distanceSquared(seed1);
                final double distToSeed2 = p.distanceSquared(seed2);

                if (distToSeed1 < distToSeed2) leftPoints.add(p);
                else rightPoints.add(p);
            }

            // Ensure that both child nodes have points
            // DO NOT USE THIS ALL THE TIME, THIS HAS O(n*log(n)) runtime, compared to the O(n) above, and also DOES NOT GUARANTEE PERFECT RESULTS!
            if (leftPoints.isEmpty() || rightPoints.isEmpty()) {
                // The initial seed choice was bad -> sorting for optimal choice.
                final List<BallTreePoint<T>> sortedPoints = new ArrayList<>(points);
                sortedPoints.sort(Comparator.comparingDouble(p -> p.distanceSquared(seed1)));

                // Split the sorted list directly.
                int medianIndex = sortedPoints.size() / 2;
                leftPoints = sortedPoints.subList(0, medianIndex);
                rightPoints = sortedPoints.subList(medianIndex, sortedPoints.size());
            }

            // Create child nodes
            leftChild = new Node<>(leftPoints);
            rightChild = new Node<>(rightPoints);

            // Clear points to save memory
            points = null;
        }

        private double distance(Node<T> other)
        {
            return distanceBetweenPoints(this.centerX, this.centerY, this.centerZ, other.centerX, other.centerY, other.centerZ);
        }
    }
}