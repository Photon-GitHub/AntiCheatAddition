package de.photon.aacadditionpro.util.datastructure;

import de.photon.aacadditionpro.util.datastructure.buffer.RingBuffer;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class BufferTest
{
    @Test
    void RingBufferInputTest()
    {
        List<String> forgottenList = new ArrayList<>();

        val buffer = new RingBuffer<String>(10)
        {
            @Override
            public void onForget(String forgotten)
            {
                forgottenList.add(forgotten);
            }
        };

        buffer.add("1");
        buffer.add("2");
        buffer.add("3");
        buffer.add("4");
        buffer.add("5");
        buffer.add("6");
        buffer.add("7");
        buffer.add("8");
        buffer.add("9");
        buffer.add("10");
        buffer.add("11");
        buffer.add("12");

        List<String> expectedForgotten = List.of("1", "2");
        List<String> expected = List.of("3", "4", "5", "6", "7", "8", "9", "10", "11", "12");

        Assertions.assertSame(expectedForgotten.size(), forgottenList.size(), "Different forgotten sizes: EXPECTED: " + expectedForgotten.size() + " ACTUAL: " + forgottenList.size());
        for (String s : forgottenList) {
            Assertions.assertTrue(expectedForgotten.contains(s), "Wrong forgotten element: " + s);
        }

        Assertions.assertSame(expected.size(), buffer.size(), "Different sizes: EXPECTED: " + expected.size() + " ACTUAL: " + buffer.size());
        for (String s : buffer) {
            Assertions.assertTrue(expected.contains(s), "Wrong element: " + s);
        }
    }

    @Test
    void RingBufferIterationTest()
    {

        val buffer = new RingBuffer<String>(10);
        buffer.add("1");
        buffer.add("2");
        buffer.add("3");
        buffer.add("4");


        List<String> expected = List.of("1", "2", "3", "4");
        int i = 0;
        for (String s : buffer) {
            Assertions.assertEquals(expected.get(i++), s);
        }

        buffer.add("5");
        buffer.add("6");
        buffer.add("7");
        buffer.add("8");
        buffer.add("9");
        buffer.add("10");
        buffer.add("11");
        buffer.add("12");

        expected = List.of("3", "4", "5", "6", "7", "8", "9", "10", "11", "12");
        i = 0;
        for (String s : buffer) {
            Assertions.assertEquals(expected.get(i++), s);
        }


        Assertions.assertSame(expected.size(), buffer.size(), "Different sizes: EXPECTED: " + expected.size() + " ACTUAL: " + buffer.size());
        int dotSize = 0;
        for (String s : buffer) {
            Assertions.assertEquals(expected.get(dotSize), s, "Wrong element: " + s);
            ++dotSize;
        }
        Assertions.assertSame(expected.size(), dotSize, "Different dot sizes: EXPECTED: " + expected.size() + " ACTUAL: " + dotSize);

        Iterator<String> ascendingIterator = buffer.iterator();
        int ascSize = 0;
        String next;
        while (ascendingIterator.hasNext()) {
            next = ascendingIterator.next();

            Assertions.assertEquals(expected.get(ascSize), next, "Wrong element: " + next);
            ++ascSize;
        }
        Assertions.assertSame(expected.size(), ascSize, "Wrong amount of elements ASC.");

        Iterator<String> descendingIterator = buffer.descendingIterator();
        int desSize = 0;
        while (descendingIterator.hasNext()) {
            next = descendingIterator.next();

            Assertions.assertEquals(expected.get(expected.size() - 1 - desSize), next, "Wrong element: " + next);
            ++desSize;
        }
        Assertions.assertSame(expected.size(), desSize, "Wrong amount of elements DESC.");
    }

    @Test
    void RingBufferArrayTest()
    {

        RingBuffer<String> buffer = new RingBuffer<>(10);
        buffer.add("1");
        buffer.add("2");
        buffer.add("3");
        buffer.add("4");
        buffer.add("5");
        buffer.add("6");
        buffer.add("7");
        buffer.add("8");
        buffer.add("9");
        buffer.add("10");
        buffer.add("11");
        buffer.add("12");

        val expected = List.of("3", "4", "5", "6", "7", "8", "9", "10", "11", "12").toArray(new String[0]);
        val actual = buffer.toArray(new String[0]);
        Assertions.assertArrayEquals(expected, actual);
    }
}
