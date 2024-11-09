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

        assertTrue(tree.rangeSearch(targetX, targetY, targetZ, radius).isEmpty(), "After removing all points, the range search should return an empty list.");
    }
}
