package de.photon.anticheataddition.util.datastructure.balltree;

import java.util.*;

/**
 * A 3D Ball Tree implementation for efficient spatial partitioning, range queries, and dynamic updates.
 *
 * @param <T> The type of the objects stored in the tree.
 */
public class ThreeDBallTree<T>
{
    public record BallTreePoint<T>(double x, double y, double z, T data)
    {
        double distanceSquared(BallTreePoint<T> other)
        {
            final double dx = other.x - x;
            final double dy = other.y - y;
            final double dz = other.z - z;
            return dx * dx + dy * dy + dz * dz;
        }
    }

    private static final int MAX_LEAF_SIZE = 8;

    private static class Node<T>
    {
        double centerX;
        double centerY;
        double centerZ;

        double radius;

        Node<T> leftChild;
        Node<T> rightChild;

        List<BallTreePoint<T>> points; // Only for leaf nodes

        public Node(List<BallTreePoint<T>> points)
        {
            this.points = points;
            computeCenterAndRadius();
        }

        private void computeCenterAndRadius()
        {
            if (points == null || points.isEmpty()) {
                // Internal node
                if (leftChild != null && rightChild != null) {
                    centerX = (leftChild.centerX + rightChild.centerX) / 2;
                    centerY = (leftChild.centerY + rightChild.centerY) / 2;
                    centerZ = (leftChild.centerZ + rightChild.centerZ) / 2;

                    double distToLeft = distanceBetweenPoints(centerX, centerY, centerZ, leftChild.centerX, leftChild.centerY, leftChild.centerZ) + leftChild.radius;
                    double distToRight = distanceBetweenPoints(centerX, centerY, centerZ, rightChild.centerX, rightChild.centerY, rightChild.centerZ) + rightChild.radius;
                    radius = Math.max(distToLeft, distToRight);
                }
                return;
            }

            // Leaf node
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

            final double maxDistanceSquared = points.parallelStream()
                                                    .mapToDouble(p -> distanceSquaredBetweenPoints(p.x(), p.y(), p.z(), centerX, centerY, centerZ))
                                                    .max()
                                                    .orElse(0);

            radius = Math.sqrt(maxDistanceSquared);
        }

        public void splitNode()
        {
            if (points.size() <= 1) {
                return; // Cannot split further
            }

            // Step 1: Choose the first point as the first seed.
            BallTreePoint<T> seed1 = points.get(0);

            // Step 2: Find the point furthest from seed1 to be seed2
            BallTreePoint<T> seed2 = null;
            double maxDistSquared = -1;
            for (BallTreePoint<T> p : points) {
                double distSquared = p.distanceSquared(seed1);
                if (distSquared > maxDistSquared) {
                    maxDistSquared = distSquared;
                    seed2 = p;
                }
            }

            if (seed2 == null || seed1.equals(seed2)) {
                return; // Cannot split
            }

            // Step 3: Assign points to the nearest seed
            List<BallTreePoint<T>> leftPoints = new ArrayList<>();
            List<BallTreePoint<T>> rightPoints = new ArrayList<>();
            for (BallTreePoint<T> p : points) {
                double distToSeed1 = p.distanceSquared(seed1);
                double distToSeed2 = p.distanceSquared(seed2);
                if (distToSeed1 < distToSeed2) {
                    leftPoints.add(p);
                } else {
                    rightPoints.add(p);
                }
            }

            // Ensure that both child nodes have points
            if (leftPoints.isEmpty() || rightPoints.isEmpty()) {
                // If one child is empty, split at median distance
                List<BallTreePoint<T>> sortedPoints = new ArrayList<>(points);
                sortedPoints.sort(Comparator.comparingDouble(p -> p.distanceSquared(seed1)));
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
    }

    private Node<T> root;

    public ThreeDBallTree(Collection<BallTreePoint<T>> points)
    {
        buildTreeIteratively(points);
    }

    private void buildTreeIteratively(Collection<BallTreePoint<T>> points)
    {
        root = new Node<T>(new ArrayList<>(points));
        Queue<Node<T>> queue = new ArrayDeque<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            Node<T> node = queue.poll();
            if (node.points.size() > MAX_LEAF_SIZE) {
                node.splitNode();
                if (node.leftChild != null) queue.add(node.leftChild);
                if (node.rightChild != null) queue.add(node.rightChild);
                node.points = null; // Clear points to save memory
            }
        }
    }

    public void insert(BallTreePoint<T> point)
    {
        final Deque<Node<T>> path = new ArrayDeque<>();
        Node<T> current = root;

        while (current.leftChild != null && current.rightChild != null) {
            path.push(current);
            double distToLeft = distanceSquaredToNode(point, current.leftChild);
            double distToRight = distanceSquaredToNode(point, current.rightChild);

            if (distToLeft < distToRight) {
                current = current.leftChild;
            } else {
                current = current.rightChild;
            }
        }

        // Leaf node
        current.points.add(point);
        current.computeCenterAndRadius();

        if (current.points.size() > MAX_LEAF_SIZE) {
            current.splitNode();
        }

        // Update centers and radii
        while (!path.isEmpty()) {
            Node<T> node = path.pop();
            node.computeCenterAndRadius();
        }
    }

    public BallTreePoint<T> get(double x, double y, double z)
    {
        final Deque<Node<T>> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            Node<T> node = stack.pop();

            double distToNode = distanceBetweenPoints(x, y, z, node.centerX, node.centerY, node.centerZ);
            if (distToNode > node.radius) {
                continue;
            }

            if (node.leftChild == null && node.rightChild == null) {
                if (node.points != null) {
                    for (BallTreePoint<T> p : node.points) {
                        if (p.x() == x && p.y() == y && p.z() == z) {
                            return p;
                        }
                    }
                }
            } else {
                if (node.leftChild != null) stack.push(node.leftChild);
                if (node.rightChild != null) stack.push(node.rightChild);
            }
        }

        return null;
    }

    public boolean remove(BallTreePoint<T> point)
    {
        final Deque<Node<T>> stack = new ArrayDeque<>();
        final Deque<Node<T>> path = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            Node<T> node = stack.pop();
            path.push(node);

            double distToNode = distanceBetweenPoints(point.x(), point.y(), point.z(), node.centerX, node.centerY, node.centerZ);
            if (distToNode > node.radius) {
                path.pop();
                continue;
            }

            if (node.leftChild == null && node.rightChild == null) {
                if (node.points != null && node.points.remove(point)) {
                    node.computeCenterAndRadius();
                    while (!path.isEmpty()) {
                        Node<T> n = path.pop();
                        n.computeCenterAndRadius();
                    }
                    return true;
                } else {
                    path.pop();
                    continue;
                }
            } else {
                if (node.leftChild != null) stack.push(node.leftChild);
                if (node.rightChild != null) stack.push(node.rightChild);
            }
        }

        return false;
    }

    public boolean contains(BallTreePoint<T> point)
    {
        return get(point.x(), point.y(), point.z()) != null;
    }

    public Set<BallTreePoint<T>> rangeSearch(double x, double y, double z, double radius)
    {
        final Set<BallTreePoint<T>> result = new HashSet<>();
        final Deque<Node<T>> stack = new ArrayDeque<>(256);
        stack.push(root);

        final double radiusSquared = radius * radius;

        while (!stack.isEmpty()) {
            final Node<T> node = stack.pop();

            final double distSquaredToNode = distanceSquaredBetweenPoints(x, y, z, node.centerX, node.centerY, node.centerZ);
            final double maxDist = node.radius + radius;

            if (distSquaredToNode > maxDist * maxDist) {
                continue;
            }

            if (node.leftChild == null && node.rightChild == null) {
                if (node.points != null) {
                    for (BallTreePoint<T> p : node.points) {
                        if (distanceSquaredBetweenPoints(x, y, z, p.x(), p.y(), p.z()) <= radiusSquared) result.add(p);
                    }
                }
            } else {
                if (node.leftChild != null) stack.push(node.leftChild);
                if (node.rightChild != null) stack.push(node.rightChild);
            }
        }

        return result;
    }

    private double distanceSquaredToNode(BallTreePoint<T> point, Node<T> node)
    {
        final double dx = point.x() - node.centerX;
        final double dy = point.y() - node.centerY;
        final double dz = point.z() - node.centerZ;
        return dx * dx + dy * dy + dz * dz;
    }

    private static double distanceBetweenPoints(double x1, double y1, double z1, double x2, double y2, double z2)
    {
        return Math.sqrt(distanceSquaredBetweenPoints(x1, y1, z1, x2, y2, z2));
    }

    private static double distanceSquaredBetweenPoints(double x1, double y1, double z1, double x2, double y2, double z2)
    {
        final double dx = x1 - x2;
        final double dy = y1 - y2;
        final double dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }
}