package de.photon.aacadditionpro.util.datastructure;

import de.photon.aacadditionpro.util.datastructure.kdtree.QuadTreeIteration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

class TestQuadTreeIteration
{
    @Test
    void emptyTest()
    {
        var quad = new QuadTreeIteration<Boolean>();
        Assertions.assertFalse(quad.iterator().hasNext());
        Assertions.assertEquals(0, quad.size());
    }

    @Test
    void oneTest()
    {
        var quad = new QuadTreeIteration<Boolean>();
        quad.add(1, 1, false);
        Assertions.assertTrue(quad.iterator().hasNext());
        Assertions.assertEquals(1, quad.size());

        var list = new ArrayList<>(quad);
        Assertions.assertEquals(1, list.size());
        var expected = new QuadTreeIteration.Node<>(1, 1, false);
        Assertions.assertEquals(expected, list.get(0));
    }

    @Test
    void removeIterationTest()
    {
        var quad = new QuadTreeIteration<Boolean>();

        for (int i = 0; i < 100; ++i) {
            quad.add(i, i, false);
        }

        int x = quad.size();
        while (!quad.isEmpty()) {
            --x;
            var any = quad.getAny();
            quad.remove(any);
            Assertions.assertEquals(x, quad.size());
        }
    }
}
