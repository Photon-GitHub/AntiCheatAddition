package de.photon.anticheataddition.util.datastructure;

import de.photon.anticheataddition.util.datastructure.balltree.ThreeDBallTree;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ThreeDBallTreeTest
{
    private static final int NUM_ELEMENTS = 100;

    @Test
    void testTreeConstructionWithValidData()
    {
        List<String> points = List.of("A", "B", "C");
        List<Vector> coordinates = List.of(new Vector(1, 2, 3), new Vector(4, 5, 6), new Vector(7, 8, 9));

        ThreeDBallTree<String> tree = new ThreeDBallTree<>(points, coordinates);

        assertNotNull(tree, "Tree should be constructed successfully.");
    }

    @Test
    void testTreeConstructionWithMismatchedPointsAndCoordinates()
    {
        List<String> points = List.of("A", "B");
        List<Vector> coordinates = List.of(new Vector(1, 2, 3));

        final Exception exception = assertThrows(IllegalArgumentException.class, () -> new ThreeDBallTree<>(points, coordinates));

        assertEquals("Each point must have one corresponding coordinate.", exception.getMessage());
    }

    @Test
    void testInsertAddsNewPoint()
    {
        List<String> points = List.of("A", "B", "C");
        List<Vector> coordinates = List.of(new Vector(1, 2, 3), new Vector(4, 5, 6), new Vector(7, 8, 9));

        ThreeDBallTree<String> tree = new ThreeDBallTree<>(points, coordinates);

        String newPoint = "D";
        Vector newCoordinate = new Vector(10, 10, 10);
        tree.insert(newPoint, newCoordinate);

        Vector target = new Vector(10, 10, 10);
        double radius = 1.0;

        List<String> result = tree.rangeSearch(target, radius);
        assertTrue(result.contains(newPoint), "Newly added point D should be found in the range search.");
    }

    @Test
    void testRemoveDeletesPoint()
    {
        List<String> points = List.of("A", "B", "C");
        List<Vector> coordinates = List.of(new Vector(1, 2, 3), new Vector(4, 5, 6), new Vector(7, 8, 9));

        ThreeDBallTree<String> tree = new ThreeDBallTree<>(points, coordinates);

        String pointToRemove = "B";
        Vector coordinateToRemove = new Vector(4, 5, 6);
        tree.remove(pointToRemove, coordinateToRemove);

        Vector target = new Vector(4, 5, 6);
        double radius = 1.0;

        List<String> result = tree.rangeSearch(target, radius);
        assertFalse(result.contains(pointToRemove), "Point B should no longer be found in the range search after removal.");
    }

    @Test
    void testRemoveNonExistentPoint()
    {
        List<String> points = List.of("A", "B", "C");
        List<Vector> coordinates = List.of(new Vector(1, 2, 3), new Vector(4, 5, 6), new Vector(7, 8, 9));

        ThreeDBallTree<String> tree = new ThreeDBallTree<>(points, coordinates);

        String nonExistentPoint = "D";
        Vector nonExistentCoordinate = new Vector(10, 10, 10);

        tree.remove(nonExistentPoint, nonExistentCoordinate);

        assertEquals(3, tree.rangeSearch(new Vector(0, 0, 0), 100.0).size(),
                     "Tree size should remain the same when attempting to remove a non-existent point.");
    }

    @Test
    void testRangeSearchReturnsCorrectResults()
    {
        List<String> points = List.of("A", "B", "C", "D");
        List<Vector> coordinates = List.of(new Vector(1, 2, 3), new Vector(4, 5, 6), new Vector(10, 10, 10), new Vector(3, 2, 1));

        ThreeDBallTree<String> tree = new ThreeDBallTree<>(points, coordinates);

        Vector target = new Vector(3, 3, 3);
        double radius = 5.0;

        List<String> result = tree.rangeSearch(target, radius);

        assertTrue(result.contains("A"), "Result should include point A.");
        assertTrue(result.contains("B"), "Result should include point B.");
        assertTrue(result.contains("D"), "Result should include point D.");
        assertFalse(result.contains("C"), "Result should not include point C.");
    }

    @Test
    void testRangeSearchWithEmptyTree()
    {
        List<String> points = List.of();
        List<Vector> coordinates = List.of();

        ThreeDBallTree<String> tree = new ThreeDBallTree<>(points, coordinates);

        Vector target = new Vector(1, 1, 1);
        double radius = 5.0;

        List<String> result = tree.rangeSearch(target, radius);

        assertTrue(result.isEmpty(), "Result should be empty for an empty tree.");
    }

    @Test
    void testRangeSearchWithRandomizedData()
    {
        Random random = new Random();

        // Generate random points and coordinates
        List<String> points = new ArrayList<>();
        List<Vector> coordinates = new ArrayList<>();

        for (int i = 0; i < NUM_ELEMENTS; i++) {
            points.add("Point" + i);
            coordinates.add(new Vector(
                    random.nextDouble() * 100,
                    random.nextDouble() * 100,
                    random.nextDouble() * 100
            ));
        }

        ThreeDBallTree<String> tree = new ThreeDBallTree<>(points, coordinates);

        // Define random target and radius
        Vector target = new Vector(
                random.nextDouble() * 100,
                random.nextDouble() * 100,
                random.nextDouble() * 100
        );
        final double radius = random.nextDouble() * 40;

        // Perform range search
        List<String> result = tree.rangeSearch(target, radius);

        // Verify results (ensure all returned points are within the radius)
        for (String point : result) {
            int index = points.indexOf(point);
            Vector coord = coordinates.get(index);
            double distance = coord.distance(target);

            assertTrue(distance <= radius, "Point " + point + " should be within the radius.");
        }

        // Verify no additional points are within the radius but not in the result
        for (int i = 0; i < NUM_ELEMENTS; i++) {
            Vector coord = coordinates.get(i);
            double distance = coord.distance(target);

            if (distance <= radius) {
                assertTrue(result.contains(points.get(i)), "Point " + points.get(i) + " should be in the result.");
            }
        }
    }
}
