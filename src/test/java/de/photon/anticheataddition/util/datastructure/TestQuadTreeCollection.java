package de.photon.anticheataddition.util.datastructure;

import de.photon.anticheataddition.util.datastructure.kdtree.QuadTreeQueue;
import de.photon.anticheataddition.util.datastructure.kdtree.QuadTreeSet;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
class TestQuadTreeCollection
{
    @Test
    void emptySetTest()
    {
        val quad = new QuadTreeSet<Boolean>();
        Assertions.assertFalse(quad.iterator().hasNext());
        Assertions.assertEquals(0, quad.size());
    }

    @Test
    void emptyQueueTest()
    {
        val quad = new QuadTreeQueue<Boolean>();
        Assertions.assertFalse(quad.iterator().hasNext());
        Assertions.assertEquals(0, quad.size());
    }

    @Test
    void clearSetTest()
    {
        val quad = new QuadTreeSet<Boolean>();

        for (int i = 0; i < 100; ++i) {
            quad.add(i, i, false);
        }

        Assertions.assertEquals(100, quad.size());
        quad.clear();
        //noinspection ConstantConditions
        Assertions.assertEquals(0, quad.size());
        Assertions.assertTrue(quad.isEmpty());
    }

    @Test
    void clearQueueTest()
    {
        val quad = new QuadTreeQueue<Boolean>();

        for (int i = 0; i < 100; ++i) {
            quad.add(i, i, false);
        }

        Assertions.assertEquals(100, quad.size());
        quad.clear();
        //noinspection ConstantConditions
        Assertions.assertEquals(0, quad.size());
        Assertions.assertTrue(quad.isEmpty());
    }

    @Test
    void oneSetTest()
    {
        val quad = new QuadTreeSet<Boolean>();
        quad.add(1, 1, false);
        Assertions.assertTrue(quad.iterator().hasNext());
        Assertions.assertEquals(1, quad.size());

        val list = new ArrayList<>(quad);
        Assertions.assertEquals(1, list.size());
        val expected = new QuadTreeSet.Node<>(1.0, 1.0, false);
        Assertions.assertEquals(expected, list.get(0));
    }

    @Test
    void oneQueueTest()
    {
        val quad = new QuadTreeQueue<Boolean>();
        quad.add(1, 1, false);
        Assertions.assertTrue(quad.iterator().hasNext());
        Assertions.assertEquals(1, quad.size());

        val list = new ArrayList<>(quad);
        Assertions.assertEquals(1, list.size());
        val expected = new QuadTreeSet.Node<>(1.0, 1.0, false);
        Assertions.assertEquals(expected, list.get(0));
    }

    @Test
    void removeSetIterationTest()
    {
        val quad = new QuadTreeSet<Boolean>();

        for (int i = 0; i < 100; ++i) {
            quad.add(i, i, false);
        }

        int x = quad.size();
        while (!quad.isEmpty()) {
            --x;
            var any = quad.removeAny();
            Assertions.assertEquals(0, quad.querySquare(any, 0.01).size());
            Assertions.assertEquals(x, quad.size());
        }
    }

    @Test
    void removeQueueIterationTest()
    {
        val quad = new QuadTreeQueue<Boolean>();

        for (int i = 0; i < 100; ++i) {
            quad.add(i, i, false);
        }

        int x = quad.size();
        while (!quad.isEmpty()) {
            --x;
            var any = quad.removeAny();
            Assertions.assertEquals(0, quad.querySquare(any, 0.01).size());
            Assertions.assertEquals(x, quad.size());
        }
    }
}
