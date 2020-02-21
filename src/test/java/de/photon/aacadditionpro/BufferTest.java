package de.photon.aacadditionpro;

import com.google.common.collect.ImmutableList;
import de.photon.aacadditionpro.util.datastructures.buffer.ContinuousArrayBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BufferTest
{
    @Test
    public void ContinuousArrayBufferInputTest()
    {
        List<String> forgottenList = new ArrayList<>();

        ContinuousArrayBuffer<String> buffer = new ContinuousArrayBuffer<String>(10)
        {
            @Override
            public void onForget(String forgotten)
            {
                forgottenList.add(forgotten);
            }
        };

        buffer.bufferObject("1");
        buffer.bufferObject("2");
        buffer.bufferObject("3");
        buffer.bufferObject("4");
        buffer.bufferObject("5");
        buffer.bufferObject("6");
        buffer.bufferObject("7");
        buffer.bufferObject("8");
        buffer.bufferObject("9");
        buffer.bufferObject("10");
        buffer.bufferObject("11");
        buffer.bufferObject("12");

        List<String> expectedForgotten = ImmutableList.of("1", "2");
        List<String> expected = ImmutableList.of("3", "4", "5", "6", "7", "8", "9", "10", "11", "12");

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
    public void ContinuousArrayBufferIterationTest()
    {

        ContinuousArrayBuffer<String> buffer = new ContinuousArrayBuffer<>(10);
        buffer.bufferObject("1");
        buffer.bufferObject("2");
        buffer.bufferObject("3");
        buffer.bufferObject("4");
        buffer.bufferObject("5");
        buffer.bufferObject("6");
        buffer.bufferObject("7");
        buffer.bufferObject("8");
        buffer.bufferObject("9");
        buffer.bufferObject("10");
        buffer.bufferObject("11");
        buffer.bufferObject("12");

        List<String> expected = ImmutableList.of("3", "4", "5", "6", "7", "8", "9", "10", "11", "12");

        Assertions.assertSame(expected.size(), buffer.size(), "Different sizes: EXPECTED: " + expected.size() + " ACTUAL: " + buffer.size());
        int dotSize = 0;
        for (String s : buffer) {
            dotSize++;
            Assertions.assertTrue(expected.contains(s), "Wrong element: " + s);
        }
        Assertions.assertSame(expected.size(), dotSize, "Different dot sizes: EXPECTED: " + expected.size() + " ACTUAL: " + dotSize);

        Iterator<String> ascendingIterator = buffer.iterator();
        int ascSize = 0;
        String next;
        while (ascendingIterator.hasNext()) {
            next = ascendingIterator.next();
            ascSize++;

            Assertions.assertTrue(expected.contains(next), "Wrong element: " + next);
        }
        Assertions.assertSame(expected.size(), ascSize, "Wrong amount of elements ASC.");

        Iterator<String> descendingIterator = buffer.descendingIterator();
        int desSize = 0;
        while (descendingIterator.hasNext()) {
            next = descendingIterator.next();
            desSize++;

            Assertions.assertTrue(expected.contains(next), "Wrong element: " + next);
        }
        Assertions.assertSame(expected.size(), desSize, "Wrong amount of elements DESC.");
    }
}
