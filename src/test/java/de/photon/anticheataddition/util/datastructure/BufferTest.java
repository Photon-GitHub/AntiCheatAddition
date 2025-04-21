package de.photon.anticheataddition.util.datastructure;

import de.photon.anticheataddition.util.datastructure.buffer.RingBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class BufferTest
{

    @Test
    void testAddAndOnForget()
    {
        List<String> forgottenList = new ArrayList<>();
        final var buffer = new RingBuffer<String>(10)
        {
            @Override
            public void onForget(String forgotten)
            {
                forgottenList.add(forgotten);
            }
        };

        // Add 12 elements, so that 2 oldest elements are overwritten.
        for (int i = 1; i <= 12; i++) {
            buffer.add(String.valueOf(i));
        }

        List<String> expectedForgotten = List.of("1", "2");
        List<String> expectedBuffer = List.of("3", "4", "5", "6", "7", "8", "9", "10", "11", "12");

        Assertions.assertEquals(expectedForgotten.size(), forgottenList.size(), "Forgotten list size mismatch");
        Assertions.assertTrue(forgottenList.containsAll(expectedForgotten), "Forgotten list does not contain the expected elements");

        Assertions.assertEquals(expectedBuffer.size(), buffer.size(), "Buffer size mismatch after additions");
        int idx = 0;
        for (String s : buffer) {
            Assertions.assertEquals(expectedBuffer.get(idx++), s, "Element mismatch at buffer index " + (idx - 1));
        }
    }

    @Test
    void testIterationOrder()
    {
        final var buffer = new RingBuffer<String>(10);
        buffer.add("1");
        buffer.add("2");
        buffer.add("3");
        buffer.add("4");

        List<String> expected = List.of("1", "2", "3", "4");
        int i = 0;
        for (String s : buffer) {
            Assertions.assertEquals(expected.get(i++), s, "Iteration order mismatch at index " + (i - 1));
        }
        Assertions.assertEquals(expected.size(), i, "Total number of iterated elements does not match");

        // Overwrite some elements by adding more items.
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
            Assertions.assertEquals(expected.get(i++), s, "Iteration order mismatch after overwrite at index " + (i - 1));
        }
        Assertions.assertEquals(expected.size(), i, "Iterated element count mismatch after overwrites");

        // Test iterator explicitly.
        Iterator<String> ascendingIterator = buffer.iterator();
        i = 0;
        while (ascendingIterator.hasNext()) {
            String next = ascendingIterator.next();
            Assertions.assertEquals(expected.get(i++), next, "Iterator returned wrong element at index " + (i - 1));
        }
        Assertions.assertEquals(expected.size(), i, "Iterator did not traverse the expected number of elements");
    }

    @Test
    void testToArrayAndClear()
    {
        final RingBuffer<String> buffer = new RingBuffer<>(10);
        // Add 12 elements so that the buffer wraps.
        for (int i = 1; i <= 12; i++) {
            buffer.add(String.valueOf(i));
        }

        String[] expectedArray = new String[]{"3", "4", "5", "6", "7", "8", "9", "10", "11", "12"};
        Assertions.assertArrayEquals(expectedArray, buffer.toArray(new String[0]), "toArray result mismatch");

        // Test clear method.
        buffer.clear();
        Assertions.assertEquals(0, buffer.size(), "Buffer size should be 0 after clear");
        Assertions.assertArrayEquals(new String[0], buffer.toArray(new String[0]), "toArray should be empty after clear");
    }

    @Test
    void testRemoveFirstAndRemoveLast()
    {
        final RingBuffer<String> buffer = new RingBuffer<>(5);
        // Fill the buffer with 5 elements.
        buffer.add("A");
        buffer.add("B");
        buffer.add("C");
        buffer.add("D");
        buffer.add("E");

        // removeFirst should remove "A"
        String removedFirst = buffer.removeFirst();
        Assertions.assertEquals("A", removedFirst, "removeFirst did not return the correct element");
        Assertions.assertEquals(4, buffer.size(), "Buffer size incorrect after removeFirst");

        // removeLast should remove "E"
        String removedLast = buffer.removeLast();
        Assertions.assertEquals("E", removedLast, "removeLast did not return the correct element");
        Assertions.assertEquals(3, buffer.size(), "Buffer size incorrect after removeLast");

        // Verify remaining order: expected "B", "C", "D"
        List<String> expected = List.of("B", "C", "D");
        int idx = 0;
        for (String s : buffer) {
            Assertions.assertEquals(expected.get(idx++), s, "Remaining element mismatch at index " + (idx - 1));
        }

        // Test exceptions when removing from an empty buffer.
        buffer.clear();
        Assertions.assertThrows(IllegalStateException.class, buffer::removeFirst, "Expected exception when calling removeFirst on an empty buffer");
        Assertions.assertThrows(IllegalStateException.class, buffer::removeLast, "Expected exception when calling removeLast on an empty buffer");
    }

    @Test
    void testGetMethodAndBoundaries()
    {
        final RingBuffer<String> buffer = new RingBuffer<>(5);
        // Test that get on an empty buffer throws an exception.
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(0), "Expected exception when calling get on an empty buffer");

        // Add elements.
        buffer.add("X");
        buffer.add("Y");
        buffer.add("Z");

        Assertions.assertEquals("X", buffer.get(0), "get(0) should return the first element");
        Assertions.assertEquals("Y", buffer.get(1), "get(1) should return the second element");
        Assertions.assertEquals("Z", buffer.get(2), "get(2) should return the third element");

        // Test out-of-bound indices.
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(-1), "Expected exception for negative index");
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(3), "Expected exception for index equal to size");
    }

    @Test
    void testPreFilledConstructor()
    {
        // Test the constructor that pre-fills the buffer.
        final RingBuffer<Integer> buffer = new RingBuffer<>(5, 42);
        Assertions.assertEquals(5, buffer.size(), "Pre-filled buffer should have size equal to maxSize");

        // All elements should be 42.
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(42, buffer.get(i), "Element at index " + i + " should be 42");
        }
    }
}
