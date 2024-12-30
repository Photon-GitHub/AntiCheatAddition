package de.photon.anticheataddition.util.datastructure;

import de.photon.anticheataddition.util.datastructure.balltree.ThreeDBallTree;
import de.photon.anticheataddition.util.datastructure.balltree.ThreeDBallTree.BallTreePoint;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ThreeDBallTreeTest
{
    private static final int NUM_ELEMENTS = 1000;

    @Test
    void testTreeConstructionWithValidData()
    {
        List<BallTreePoint<String>> points = List.of(new BallTreePoint<>(1, 2, 3, "A"),
                                                     new BallTreePoint<>(4, 5, 6, "B"),
                                                     new BallTreePoint<>(7, 8, 9, "C"));

        ThreeDBallTree<String> tree = new ThreeDBallTree<>(points);

        assertNotNull(tree, "Tree should be constructed successfully.");
    }

    @Test
    void testTreeConstructionWithEmptyData()
    {
        List<BallTreePoint<String>> points = List.of();

        ThreeDBallTree<String> tree = new ThreeDBallTree<>(points);

        assertNotNull(tree, "Tree should be constructed successfully even with empty data.");
    }

    @Test
    void testInsertAddsNewPoint()
    {
        List<BallTreePoint<String>> points = List.of(new BallTreePoint<>(1, 2, 3, "A"),
                                                     new BallTreePoint<>(4, 5, 6, "B"),
                                                     new BallTreePoint<>(7, 8, 9, "C"));

        ThreeDBallTree<String> tree = new ThreeDBallTree<>(points);

        BallTreePoint<String> newPoint = new BallTreePoint<>(10, 10, 10, "D");
        tree.insert(newPoint);

        Set<BallTreePoint<String>> result = tree.rangeSearch(10, 10, 10, 1.0);
        assertTrue(result.contains(newPoint), "Newly added point D should be found in the range search.");
    }

    @Test
    void testRemoveDeletesPoint()
    {
        List<BallTreePoint<String>> points = List.of(new BallTreePoint<>(1, 2, 3, "A"),
                                                     new BallTreePoint<>(4, 5, 6, "B"),
                                                     new BallTreePoint<>(7, 8, 9, "C"));

        ThreeDBallTree<String> tree = new ThreeDBallTree<>(points);

        BallTreePoint<String> pointToRemove = new BallTreePoint<>(4, 5, 6, "B");

        Set<BallTreePoint<String>> result = tree.rangeSearch(4, 5, 6, 1.0);
        assertTrue(result.contains(pointToRemove), "Point B should be in the tree initially.");

        tree.remove(pointToRemove);

        result = tree.rangeSearch(4, 5, 6, 1.0);
        assertFalse(result.contains(pointToRemove), "Point B should no longer be found in the range search after removal.");
    }

    @Test
    void testRemoveNonExistentPoint()
    {
        List<BallTreePoint<String>> points = List.of(new BallTreePoint<>(1, 2, 3, "A"),
                                                     new BallTreePoint<>(4, 5, 6, "B"),
                                                     new BallTreePoint<>(7, 8, 9, "C"));

        ThreeDBallTree<String> tree = new ThreeDBallTree<>(points);

        BallTreePoint<String> nonExistentPoint = new BallTreePoint<>(10, 10, 10, "D");

        boolean result = tree.remove(nonExistentPoint);

        assertFalse(result, "Removing a non-existent point should return false.");
    }

    @Test
    void testRangeSearchReturnsCorrectResults()
    {
        List<BallTreePoint<String>> points = List.of(new BallTreePoint<>(1, 2, 3, "A"),
                                                     new BallTreePoint<>(4, 5, 6, "B"),
                                                     new BallTreePoint<>(10, 10, 10, "C"),
                                                     new BallTreePoint<>(3, 2, 1, "D"));

        ThreeDBallTree<String> tree = new ThreeDBallTree<>(points);

        Set<BallTreePoint<String>> result = tree.rangeSearch(3, 3, 3, 5.0);

        assertTrue(result.stream().anyMatch(p -> "A".equals(p.data())), "Result should include point A.");
        assertTrue(result.stream().anyMatch(p -> "B".equals(p.data())), "Result should include point B.");
        assertTrue(result.stream().anyMatch(p -> "D".equals(p.data())), "Result should include point D.");
        assertFalse(result.stream().anyMatch(p -> "C".equals(p.data())), "Result should not include point C.");
    }

    @Test
    void testRangeSearchWithEmptyTree()
    {
        List<BallTreePoint<String>> points = List.of();

        ThreeDBallTree<String> tree = new ThreeDBallTree<>(points);

        Set<BallTreePoint<String>> result = tree.rangeSearch(1, 1, 1, 5.0);

        assertTrue(result.isEmpty(), "Result should be empty for an empty tree.");
    }

    @Test
    void testRangeSearchWithRandomizedData()
    {
        Random random = new Random();

        // Generate random points
        List<BallTreePoint<String>> points = new ArrayList<>();
        for (int i = 0; i < NUM_ELEMENTS; i++) {
            points.add(new BallTreePoint<>(random.nextDouble() * 100, random.nextDouble() * 100, random.nextDouble() * 100, "Point" + i));
        }

        ThreeDBallTree<String> tree = new ThreeDBallTree<>(points);

        // Define random target and radius
        double targetX = random.nextDouble() * 100;
        double targetY = random.nextDouble() * 100;
        double targetZ = random.nextDouble() * 100;
        double radius = Math.abs(random.nextGaussian() + 2) * 20;

        Set<BallTreePoint<String>> result = tree.rangeSearch(targetX, targetY, targetZ, radius);

        // Verify results
        for (BallTreePoint<String> point : result) {
            double distance = Math.sqrt(Math.pow(point.x() - targetX, 2) + Math.pow(point.y() - targetY, 2) + Math.pow(point.z() - targetZ, 2));
            assertTrue(distance <= radius, "Point " + point.data() + " should be within the radius.");
        }
    }

    @Test
    void testVerifyRangeSearchWithRandomizedData()
    {
        Random random = new Random();

        // Generate random points
        List<BallTreePoint<String>> points = new ArrayList<>();
        for (int i = 0; i < NUM_ELEMENTS; i++) {
            points.add(new BallTreePoint<>(random.nextDouble() * 100, random.nextDouble() * 100, random.nextDouble() * 100, "Point" + i));
        }

        ThreeDBallTree<String> tree = new ThreeDBallTree<>(points);

        // Define random target and radius
        double targetX = random.nextDouble() * 100;
        double targetY = random.nextDouble() * 100;
        double targetZ = random.nextDouble() * 100;
        double radius = Math.abs(random.nextGaussian() + 2) * 20;

        Set<BallTreePoint<String>> result = tree.rangeSearch(targetX, targetY, targetZ, radius);

        // Verify results
        Set<BallTreePoint<String>> expectedResult = new HashSet<>();
        for (BallTreePoint<String> point : points) {
            final double distance = Math.sqrt(Math.pow(point.x() - targetX, 2) + Math.pow(point.y() - targetY, 2) + Math.pow(point.z() - targetZ, 2));
            if (distance <= radius) {
                expectedResult.add(point);
            }
        }

        assertEquals(expectedResult, result, "The rangeSearch result does not match the manual distance search results.");
    }

    @Test
    void testRangeSearchRemoveCombinationWithRandomizedData()
    {
        Random random = new Random();

        // Generate random points
        List<BallTreePoint<String>> points = new ArrayList<>();
        for (int i = 0; i < NUM_ELEMENTS; i++) {
            points.add(new BallTreePoint<>(random.nextDouble() * 100, random.nextDouble() * 100, random.nextDouble() * 100, "Point" + i));
        }

        ThreeDBallTree<String> tree = new ThreeDBallTree<>(points);

        // Define random target and radius
        double targetX = random.nextDouble() * 100;
        double targetY = random.nextDouble() * 100;
        double targetZ = random.nextDouble() * 100;
        double radius = Math.abs(random.nextGaussian() + 2) * 20;

        Set<BallTreePoint<String>> result = tree.rangeSearch(targetX, targetY, targetZ, radius);

        for (BallTreePoint<String> point : result) {
            tree.remove(point);

            // Ensure the removed point is no longer in range search results
            Set<BallTreePoint<String>> updatedResult = tree.rangeSearch(targetX, targetY, targetZ, radius);
            assertFalse(updatedResult.contains(point), "Removed point " + point.data() + " should not appear in subsequent range searches.");
        }

        assertTrue(tree.rangeSearch(targetX, targetY, targetZ, radius).isEmpty(), "After removing all points, the range search should return an empty set.");
    }

    @Test
    void testRangeSearchBoundaryConditions()
    {
        // Create points at known distances
        BallTreePoint<String> centerPoint = new BallTreePoint<>(0, 0, 0, "Center");
        BallTreePoint<String> boundaryPoint = new BallTreePoint<>(3, 4, 0, "Boundary"); // Distance 5
        BallTreePoint<String> outsidePoint = new BallTreePoint<>(6, 8, 0, "Outside"); // Distance 10

        List<BallTreePoint<String>> points = List.of(centerPoint, boundaryPoint, outsidePoint);

        ThreeDBallTree<String> tree = new ThreeDBallTree<>(points);

        double radius = 5.0;

        Set<BallTreePoint<String>> result = tree.rangeSearch(0, 0, 0, radius);

        // Assuming points at exactly the radius distance are included
        assertTrue(result.contains(centerPoint), "Center point should be included.");
        assertTrue(result.contains(boundaryPoint), "Boundary point should be included.");
        assertFalse(result.contains(outsidePoint), "Outside point should not be included.");
    }

    @Test
    void testInsertDuplicatePoints()
    {
        List<BallTreePoint<String>> points = new ArrayList<>();
        BallTreePoint<String> point1 = new BallTreePoint<>(1, 1, 1, "Point1");
        BallTreePoint<String> point2 = new BallTreePoint<>(1, 1, 1, "Point2");

        points.add(point1);

        ThreeDBallTree<String> tree = new ThreeDBallTree<>(points);

        // Insert the same coordinates with different data
        tree.insert(point2);

        int size = tree.size();
        assertEquals(2, size, "Tree should contain two points after inserting a duplicate coordinate with different data.");

        // Now, test that both points are found
        Set<BallTreePoint<String>> result = tree.rangeSearch(1, 1, 1, 0.0);

        assertEquals(2, result.size(), "Range search should return two points at the same location.");

        // Now, remove one of the points
        boolean removed = tree.remove(point1);
        assertTrue(removed, "Point1 should be removed successfully.");

        // Now, check that the other point still exists
        result = tree.rangeSearch(1, 1, 1, 0.0);

        assertEquals(1, result.size(), "After removing one point, one should remain.");
        assertTrue(result.contains(point2), "Point2 should still be in the tree.");

        // Remove the second point
        removed = tree.remove(point2);
        assertTrue(removed, "Point2 should be removed successfully.");

        // Now, the tree should be empty
        assertEquals(0, tree.size(), "Tree should be empty after removing all points.");
        assertTrue(tree.isEmpty(), "Tree should be empty.");
    }

    @Test
    void testSizeAfterInsertionsAndRemovals()
    {
        ThreeDBallTree<String> tree = new ThreeDBallTree<>(new ArrayList<>());

        assertEquals(0, tree.size(), "Tree should be empty initially.");

        BallTreePoint<String> pointA = new BallTreePoint<>(1, 2, 3, "A");
        BallTreePoint<String> pointB = new BallTreePoint<>(4, 5, 6, "B");
        BallTreePoint<String> pointC = new BallTreePoint<>(7, 8, 9, "C");

        tree.insert(pointA);
        assertEquals(1, tree.size(), "Tree should have size 1 after one insertion.");

        tree.insert(pointB);
        assertEquals(2, tree.size(), "Tree should have size 2 after two insertions.");

        tree.insert(pointC);
        assertEquals(3, tree.size(), "Tree should have size 3 after three insertions.");

        tree.remove(pointB);
        assertEquals(2, tree.size(), "Tree should have size 2 after removing one point.");

        tree.remove(pointA);
        assertEquals(1, tree.size(), "Tree should have size 1 after removing another point.");

        tree.remove(pointC);
        assertEquals(0, tree.size(), "Tree should be empty after removing all points.");

        // Try removing from empty tree
        boolean removed = tree.remove(pointA);
        assertFalse(removed, "Removing from empty tree should return false.");
    }

    @Test
    void testStressTestWithLargeNumberOfPoints()
    {
        Random random = new Random();

        List<BallTreePoint<String>> points = new ArrayList<>();

        for (int i = 0; i < NUM_ELEMENTS; i++) {
            points.add(new BallTreePoint<>(random.nextDouble() * 1000, random.nextDouble() * 1000, random.nextDouble() * 1000, "Point" + i));
        }

        ThreeDBallTree<String> tree = new ThreeDBallTree<>(points);

        assertEquals(NUM_ELEMENTS, tree.size(), "Tree should contain all inserted points.");

        // Perform a range search in the middle of the coordinate space
        double targetX = 500;
        double targetY = 500;
        double targetZ = 500;
        double radius = 50;

        Set<BallTreePoint<String>> result = tree.rangeSearch(targetX, targetY, targetZ, radius);

        // Since points are random, we cannot predict the number of results, but we can check that all results are within radius
        for (BallTreePoint<String> point : result) {
            double distanceSquared = Math.pow(point.x() - targetX, 2) + Math.pow(point.y() - targetY, 2) + Math.pow(point.z() - targetZ, 2);
            assertTrue(distanceSquared <= radius * radius, "Point " + point.data() + " should be within the radius.");
        }

        // Now, remove some points
        int pointsToRemove = Math.min(NUM_ELEMENTS, 500);
        for (int i = 0; i < pointsToRemove; i++) {
            tree.remove(points.get(i));
        }

        assertEquals(NUM_ELEMENTS - pointsToRemove, tree.size(), "Tree size should decrease after removals.");
    }

    @Test
    void testInsertAndRemoveLoop()
    {
        ThreeDBallTree<String> tree = new ThreeDBallTree<>(new ArrayList<>());

        List<BallTreePoint<String>> points = new ArrayList<>();

        for (int i = 0; i < NUM_ELEMENTS; i++) {
            BallTreePoint<String> point = new BallTreePoint<>(i, i, i, "Point" + i);
            tree.insert(point);
            points.add(point);
            assertEquals(points.size(), tree.size(), "Tree size should be " + (points.size()) + " after insertion.");

            if (i % 10 == 0 && i > 0) {
                // Remove a point
                BallTreePoint<String> pointToRemove = points.get(i / 2);
                boolean removed = tree.remove(pointToRemove);
                assertTrue(removed, "Point " + pointToRemove.data() + " should be removed.");
                points.remove(pointToRemove);
                assertEquals(points.size(), tree.size(), "Tree size should be " + points.size() + " after removal.");
            }
        }
    }

    @Test
    void testGetAnyThrowsWhenTreeIsEmpty()
    {
        final var tree = new ThreeDBallTree<Byte>();
        assertThrows(NoSuchElementException.class, tree::getAny);
    }

    @Test
    void testGetIsNullWhenTreeIsEmpty()
    {
        final var tree = new ThreeDBallTree<Byte>();
        assertNull(tree.get(0, 0, 0));
    }

    @Test
    void testGetMethodRetrievesCorrectPoint()
    {
        ThreeDBallTree<String> tree = new ThreeDBallTree<>(new ArrayList<>());

        BallTreePoint<String> pointA = new BallTreePoint<>(1, 2, 3, "A");
        BallTreePoint<String> pointB = new BallTreePoint<>(4, 5, 6, "B");

        tree.insert(pointA);
        tree.insert(pointB);

        BallTreePoint<String> retrievedPoint = tree.get(1, 2, 3);

        assertNotNull(retrievedPoint, "Point A should be retrieved.");
        assertEquals(pointA, retrievedPoint, "Retrieved point should be Point A.");

        retrievedPoint = tree.get(4, 5, 6);
        assertNotNull(retrievedPoint, "Point B should be retrieved.");
        assertEquals(pointB, retrievedPoint, "Retrieved point should be Point B.");

        // Test getting a non-existent point
        retrievedPoint = tree.get(7, 8, 9);
        assertNull(retrievedPoint, "Non-existent point should return null.");
    }

    @Test
    void testNegativeCoordinates()
    {
        List<BallTreePoint<String>> points = List.of(new BallTreePoint<>(-1, -2, -3, "A"), new BallTreePoint<>(-4, -5, -6, "B"), new BallTreePoint<>(-7, -8, -9, "C"));

        ThreeDBallTree<String> tree = new ThreeDBallTree<>(points);

        Set<BallTreePoint<String>> result = tree.rangeSearch(-4, -5, -6, 1.0);

        assertTrue(result.stream().anyMatch(p -> "B".equals(p.data())), "Result should include point B.");

        // Test inserting and removing negative coordinate points
        BallTreePoint<String> pointD = new BallTreePoint<>(-10, -10, -10, "D");
        tree.insert(pointD);

        result = tree.rangeSearch(-10, -10, -10, 0.0);
        assertTrue(result.contains(pointD), "Point D should be found after insertion.");

        boolean removed = tree.remove(pointD);
        assertTrue(removed, "Point D should be removed successfully.");

        result = tree.rangeSearch(-10, -10, -10, 0.0);
        assertFalse(result.contains(pointD), "Point D should not be found after removal.");
    }

    @Test
    void testRemovePointWithSameCoordinates()
    {
        BallTreePoint<String> pointA = new BallTreePoint<>(1, 1, 1, "A");
        BallTreePoint<String> pointB = new BallTreePoint<>(1, 1, 1, "B");

        List<BallTreePoint<String>> points = List.of(pointA, pointB);

        ThreeDBallTree<String> tree = new ThreeDBallTree<>(points);

        assertEquals(2, tree.size(), "Tree should have size 2.");

        // Remove one point
        boolean removed = tree.remove(pointA);
        assertTrue(removed, "Point A should be removed.");

        assertEquals(1, tree.size(), "Tree should have size 1 after removing one point.");

        // Check that the other point still exists
        Set<BallTreePoint<String>> result = tree.rangeSearch(1, 1, 1, 0.0);
        assertEquals(1, result.size(), "One point should remain at (1,1,1).");

        // Remove the second point
        removed = tree.remove(pointB);
        assertTrue(removed, "Point B should be removed.");

        assertEquals(0, tree.size(), "Tree should be empty after removing all points.");
    }

    @Test
    void testIterativeClusterRemovalUsingGetAny()
    {
        // Define clusters with distinct centers
        List<BallTreePoint<String>> cluster1 = Arrays.asList(
                new BallTreePoint<>(0, 0, 0, "C1P1"),
                new BallTreePoint<>(1, 1, 1, "C1P2"),
                new BallTreePoint<>(-1, -1, -1, "C1P3")
                                                            );

        List<BallTreePoint<String>> cluster2 = Arrays.asList(
                new BallTreePoint<>(100, 100, 100, "C2P1"),
                new BallTreePoint<>(101, 101, 101, "C2P2"),
                new BallTreePoint<>(99, 99, 99, "C2P3")
                                                            );

        List<BallTreePoint<String>> cluster3 = Arrays.asList(
                new BallTreePoint<>(-100, -100, -100, "C3P1"),
                new BallTreePoint<>(-101, -101, -101, "C3P2"),
                new BallTreePoint<>(-99, -99, -99, "C3P3")
                                                            );

        // Combine all clusters into a single list
        List<BallTreePoint<String>> allPoints = new ArrayList<>();
        allPoints.addAll(cluster1);
        allPoints.addAll(cluster2);
        allPoints.addAll(cluster3);

        // Initialize the Ball Tree with all points
        ThreeDBallTree<String> tree = new ThreeDBallTree<>(allPoints);

        // Define the cluster radius (should be large enough to include all points within a cluster)
        double clusterRadius = 5.0;

        // Expected number of clusters
        int expectedClusters = 3;
        int removedClusters = 0;

        // Keep track of removed points to ensure all are removed correctly
        Set<BallTreePoint<String>> removedPoints = new HashSet<>();

        // Loop until the tree is empty
        while (!tree.isEmpty()) {
            // Retrieve an arbitrary point from the tree
            BallTreePoint<String> anyPoint = tree.getAny();
            assertNotNull(anyPoint, "getAny() should return a non-null point when the tree is not empty.");

            // Perform a range search around the retrieved point
            Set<BallTreePoint<String>> clusterPoints = tree.rangeSearch(anyPoint, clusterRadius);
            assertFalse(clusterPoints.isEmpty(), "Range search should return at least one point.");

            // Remove all points found in the range search
            boolean modified = tree.removePoints(clusterPoints);
            assertTrue(modified, "removePoints should return true when points are removed.");

            // Add removed points to the tracking set
            removedPoints.addAll(clusterPoints);

            // Increment the cluster removal count
            removedClusters++;
        }

        // Verify that all clusters have been removed
        assertEquals(expectedClusters, removedClusters, "The number of removed clusters should match the expected number.");

        // Verify that all points have been removed
        assertTrue(removedPoints.containsAll(allPoints), "All points should have been removed from the tree.");
        assertEquals(0, tree.size(), "The tree size should be 0 after all clusters are removed.");
        assertTrue(tree.isEmpty(), "The tree should be empty after all clusters are removed.");
    }

    @Test
    void testIterativeClusterRemovalWithLargeNumberOfPoints()
    {
        // Define parameters for cluster creation
        int numberOfClusters = 100;
        int pointsPerCluster = 10;
        double clusterSpacing = 100.0; // Distance between cluster centers to avoid overlap
        double clusterRadius = 10.0;   // Radius for range searches to encompass the entire cluster

        List<BallTreePoint<String>> allPoints = new ArrayList<>();

        // Create 100 clusters, each with 10 points
        for (int clusterIndex = 0; clusterIndex < numberOfClusters; clusterIndex++) {
            double centerX = clusterIndex * clusterSpacing;
            double centerY = clusterIndex * clusterSpacing;
            double centerZ = clusterIndex * clusterSpacing;

            for (int pointIndex = 0; pointIndex < pointsPerCluster; pointIndex++) {
                // Distribute points around the cluster center within a small offset to form a tight cluster
                double offsetX = (pointIndex % 3) * 1.0; // 0, 1, or 2
                double offsetY = (pointIndex / 3 % 3) * 1.0; // 0, 1, or 2
                double offsetZ = (pointIndex / 9) * 1.0; // 0 or 1

                allPoints.add(new BallTreePoint<>(
                        centerX + offsetX,
                        centerY + offsetY,
                        centerZ + offsetZ,
                        "Cluster" + clusterIndex + "Point" + pointIndex
                ));
            }
        }

        // Initialize the Ball Tree with all points
        ThreeDBallTree<String> tree = new ThreeDBallTree<>(allPoints);

        // Verify initial tree size
        assertEquals(numberOfClusters * pointsPerCluster, tree.size(), "Tree should contain all inserted points initially.");

        // Keep track of removed points to ensure all are removed correctly
        int removedClusters = 0;
        Set<BallTreePoint<String>> removedPoints = new HashSet<>();

        // Loop until the tree is empty
        while (!tree.isEmpty()) {
            // Retrieve an arbitrary point from the tree
            BallTreePoint<String> anyPoint = tree.getAny();
            assertNotNull(anyPoint, "getAny() should return a non-null point when the tree is not empty.");

            // Perform a range search around the retrieved point
            Set<BallTreePoint<String>> clusterPoints = tree.rangeSearch(anyPoint, clusterRadius);
            assertFalse(clusterPoints.isEmpty(), "Range search should return at least one point.");

            // Remove all points found in the range search
            boolean modified = tree.removePoints(clusterPoints);
            assertTrue(modified, "removePoints should return true when points are removed.");

            // Add removed points to the tracking set
            removedPoints.addAll(clusterPoints);

            // Increment the cluster removal count
            removedClusters++;
        }

        // Verify that all clusters have been removed
        assertEquals(numberOfClusters, removedClusters, "The number of removed clusters should match the expected number.");

        // Verify that all points have been removed
        assertEquals(numberOfClusters * pointsPerCluster, removedPoints.size(), "All points should have been removed from the tree.");
        assertTrue(removedPoints.containsAll(allPoints), "All points should have been removed from the tree.");
        assertEquals(0, tree.size(), "The tree size should be 0 after all clusters are removed.");
        assertTrue(tree.isEmpty(), "The tree should be empty after all clusters are removed.");
    }

    @SuppressWarnings("UseBulkOperation")
    @Test
    void testIterator()
    {
        List<BallTreePoint<String>> points = List.of(new BallTreePoint<>(1, 2, 3, "A"), new BallTreePoint<>(4, 5, 6, "B"), new BallTreePoint<>(7, 8, 9, "C"));

        ThreeDBallTree<String> tree = new ThreeDBallTree<>(points);

        List<String> dataList = new ArrayList<>();

        for (String data : tree) {
            dataList.add(data);
        }

        assertEquals(3, dataList.size(), "Iterator should return all elements.");
        assertTrue(dataList.contains("A"), "Iterator should include 'A'.");
        assertTrue(dataList.contains("B"), "Iterator should include 'B'.");
        assertTrue(dataList.contains("C"), "Iterator should include 'C'.");
    }
}
