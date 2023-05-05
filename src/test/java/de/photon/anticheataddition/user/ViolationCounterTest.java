package de.photon.anticheataddition.user;

import de.photon.anticheataddition.user.data.ViolationCounter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViolationCounterTest
{
    @Test
    void testConstructorWithThreshold()
    {
        final long threshold = 5;
        final var counter = new ViolationCounter(threshold);
        assertEquals(0, counter.getCounter());
    }

    @Test
    void testCompareThreshold()
    {
        final var counter = new ViolationCounter(5);
        assertFalse(counter.compareThreshold());
        counter.increment();
        assertFalse(counter.compareThreshold());
        counter.increment();
        assertFalse(counter.compareThreshold());
        counter.increment();
        assertFalse(counter.compareThreshold());
        counter.increment();
        assertFalse(counter.compareThreshold());
        counter.increment();
        assertTrue(counter.compareThreshold());
    }

    @Test
    void testIncrementCompareThreshold()
    {
        final long threshold = 4;
        final var counter = new ViolationCounter(threshold);
        for (int i = 0; i < threshold - 1; i++) assertFalse(counter.incrementCompareThreshold());
        assertEquals(counter.getCounter(), threshold - 1);
        assertTrue(counter.incrementCompareThreshold());
    }

    @Test
    void testConditionallyIncDec()
    {
        final var counter = new ViolationCounter(3);
        counter.increment();
        counter.increment();

        for (int i = 0; i < 5; i++) assertFalse(counter.conditionallyIncDec(false));
        assertFalse(counter.conditionallyIncDec(true));
        assertEquals(1, counter.getCounter());
        assertFalse(counter.conditionallyIncDec(true));
        assertEquals(2, counter.getCounter());
        assertFalse(counter.conditionallyIncDec(false));
        assertEquals(1, counter.getCounter());
        assertFalse(counter.conditionallyIncDec(true));
        assertEquals(2, counter.getCounter());
        assertTrue(counter.conditionallyIncDec(true));
        assertEquals(3, counter.getCounter());
    }

    @Test
    void testDecrementAboveZero()
    {
        final var counter = new ViolationCounter(2);
        counter.increment();
        counter.increment();

        counter.decrementAboveZero();
        assertEquals(1, counter.getCounter());
        counter.decrementAboveZero();
        assertEquals(0, counter.getCounter());
        counter.decrementAboveZero();
        assertEquals(0, counter.getCounter());
    }

    @Test
    void testSetToZero()
    {
        final var counter = new ViolationCounter(5);
        counter.increment();
        counter.setToZero();
        assertEquals(0, counter.getCounter());
    }
}
