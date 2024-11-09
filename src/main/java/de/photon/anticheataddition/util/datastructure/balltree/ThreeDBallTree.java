package de.photon.anticheataddition.util.datastructure.balltree;

import java.util.*;

/**
 * A 3D Ball Tree implementation for efficient spatial partitioning, range queries, and dynamic updates.
 *
 * @param <T> The type of the objects stored in the tree.
 */
public class ThreeDBallTree<T>
{
    public record BallTreePoint(double x, double y, double z, Object data)
    {
        double distanceSquared(BallTreePoint other)
        {
            final double dx = other.x - x;
            final double dy = other.y - y;
            final double dz = other.z - z;
            return dx * dx + dy * dy + dz * dz;
        }
    }

    private static final int MAX_LEAF_SIZE = 10;

    private static class Node
    {
        double centerX;
        double centerY;
        double centerZ;

        double radius;

        Node leftChild;
        Node rightChild;

        List<BallTreePoint> points; // Only for leaf nodes

        public Node(List<BallTreePoint> points)
        {
            this.points = points;
            computeCenterAndRadius();
        }

        private void computeCenterAndRadius()
        {
            if (points != null && !points.isEmpty()) {
                // Leaf node
                final double[] sum = new double[3];

                for (BallTreePoint p : points) {
                    sum[0] += p.x();
                    sum[1] += p.y();
                    sum[2] += p.z();
                }

                final int n = points.size();
                centerX = sum[0] / n;
                centerY = sum[1] / n;
                centerZ = sum[2] / n;

                final double maxDistanceSquared = points.stream()
                                                        .mapToDouble(p -> distanceSquaredBetweenPoints(p.x(), p.y(), p.z(), centerX, centerY, centerZ))
                                                        .max()
                                                        .orElse(0);

                radius = Math.sqrt(maxDistanceSquared);
            } else {
                // Internal node
                if (leftChild != null && rightChild != null) {
                    centerX = (leftChild.centerX + rightChild.centerX) / 2;
                    centerY = (leftChild.centerY + rightChild.centerY) / 2;
                    centerZ = (leftChild.centerZ + rightChild.centerZ) / 2;

                    double distToLeft = distanceBetweenPoints(centerX, centerY, centerZ, leftChild.centerX, leftChild.centerY, leftChild.centerZ) + leftChild.radius;
                    double distToRight = distanceBetweenPoints(centerX, centerY, centerZ, rightChild.centerX, rightChild.centerY, rightChild.centerZ) + rightChild.radius;
                    radius = Math.max(distToLeft, distToRight);
                }
            }
        }

        public void splitNode()
        {
            // Find the two points that are furthest apart
            BallTreePoint seed1 = null;
            BallTreePoint seed2 = null;
            double maxDistSquared = -1;
            for (int i = 0; i < points.size(); i++) {
                BallTreePoint p1 = points.get(i);
                for (int j = i + 1; j < points.size(); j++) {
                    BallTreePoint p2 = points.get(j);
                    double distSquared = p1.distanceSquared(p2);
                    if (distSquared > maxDistSquared) {
                        maxDistSquared = distSquared;
                        seed1 = p1;
                        seed2 = p2;
                    }
                }
            }

            if (seed1 == null || seed2 == null || seed1.equals(seed2)) {
                // Cannot split
                return;
            }

            // Assign points to the nearest seed
            List<BallTreePoint> leftPoints = new ArrayList<>();
            List<BallTreePoint> rightPoints = new ArrayList<>();
            for (BallTreePoint p : points) {
                double distToSeed1 = p.distanceSquared(seed1);
                double distToSeed2 = p.distanceSquared(seed2);
                if (distToSeed1 < distToSeed2) {
                    leftPoints.add(p);
                } else {
                    rightPoints.add(p);
                }
            }

            // Check if splitting is possible
            if (leftPoints.isEmpty() || rightPoints.isEmpty()) {
                return;
            }

            // Create child nodes
            leftChild = new Node(leftPoints);
            rightChild = new Node(rightPoints);
            // Clear points to save memory
            points = null;
        }
    }

    private Node root;

    public ThreeDBallTree(Collection<BallTreePoint> points)
    {
        buildTreeIteratively(points);
    }

    private void buildTreeIteratively(Collection<BallTreePoint> points)
    {
        root = new Node(new ArrayList<>(points));
        Queue<Node> queue = new ArrayDeque<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            Node node = queue.poll();
            if (node.points.size() > MAX_LEAF_SIZE) {
                node.splitNode();
                if (node.leftChild != null) queue.add(node.leftChild);
                if (node.rightChild != null) queue.add(node.rightChild);
                node.points = null; // Clear points to save memory
            }
        }
    }

    public void insert(BallTreePoint point)
    {
        final Deque<Node> path = new ArrayDeque<>();
        Node current = root;

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
            Node node = path.pop();
            node.computeCenterAndRadius();
        }
    }

    public BallTreePoint get(double x, double y, double z)
    {
        final Deque<Node> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            Node node = stack.pop();

            double distToNode = distanceBetweenPoints(x, y, z, node.centerX, node.centerY, node.centerZ);
            if (distToNode > node.radius) {
                continue;
            }

            if (node.leftChild == null && node.rightChild == null) {
                if (node.points != null) {
                    for (BallTreePoint p : node.points) {
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

    public boolean remove(BallTreePoint point)
    {
        final Deque<Node> stack = new ArrayDeque<>();
        final Deque<Node> path = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            Node node = stack.pop();
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
                        Node n = path.pop();
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

    public boolean contains(BallTreePoint point)
    {
        return get(point.x(), point.y(), point.z()) != null;
    }

    public List<BallTreePoint> rangeSearch(double x, double y, double z, double radius)
    {
        List<BallTreePoint> result = new ArrayList<>();
        final Deque<Node> stack = new ArrayDeque<>();
        stack.push(root);

        double radiusSquared = radius * radius;

        while (!stack.isEmpty()) {
            Node node = stack.pop();

            double distSquaredToNode = distanceSquaredBetweenPoints(x, y, z,
                                                                    node.centerX, node.centerY, node.centerZ);
            double maxDist = node.radius + radius;
            if (distSquaredToNode > maxDist * maxDist) {
                continue;
            }

            if (node.leftChild == null && node.rightChild == null) {
                if (node.points != null) {
                    for (BallTreePoint p : node.points) {
                        double distSquaredToPoint = distanceSquaredBetweenPoints(x, y, z,
                                                                                 p.x(), p.y(), p.z());
                        if (distSquaredToPoint <= radiusSquared) {
                            result.add(p);
                        }
                    }
                }
            } else {
                if (node.leftChild != null) stack.push(node.leftChild);
                if (node.rightChild != null) stack.push(node.rightChild);
            }
        }

        return result;
    }

    private double distanceSquaredToNode(BallTreePoint point, Node node)
    {
        double dx = point.x() - node.centerX;
        double dy = point.y() - node.centerY;
        double dz = point.z() - node.centerZ;
        return dx * dx + dy * dy + dz * dz;
    }

    private static double distanceBetweenPoints(double x1, double y1, double z1, double x2, double y2, double z2)
    {
        return Math.sqrt(distanceSquaredBetweenPoints(x1, y1, z1, x2, y2, z2));
    }

    private static double distanceSquaredBetweenPoints(double x1, double y1, double z1, double x2, double y2, double z2)
    {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }
}