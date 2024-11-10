package de.photon.anticheataddition.util.datastructure;

import de.photon.anticheataddition.util.datastructure.balltree.ThreeDBallTree;
import de.photon.anticheataddition.util.datastructure.balltree.ThreeDBallTree.BallTreePoint;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

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

        int largeNumber = 100_000; // Adjust the number based on performance considerations

        for (int i = 0; i < largeNumber; i++) {
            points.add(new BallTreePoint<>(random.nextDouble() * 1000, random.nextDouble() * 1000, random.nextDouble() * 1000, "Point" + i));
        }

        ThreeDBallTree<String> tree = new ThreeDBallTree<>(points);

        assertEquals(largeNumber, tree.size(), "Tree should contain all inserted points.");

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
        int pointsToRemove = 1000;
        for (int i = 0; i < pointsToRemove; i++) {
            tree.remove(points.get(i));
        }

        assertEquals(largeNumber - pointsToRemove, tree.size(), "Tree size should decrease after removals.");
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
            assertEquals(i + 1, tree.size(), "Tree size should be " + (i + 1) + " after insertion.");

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
