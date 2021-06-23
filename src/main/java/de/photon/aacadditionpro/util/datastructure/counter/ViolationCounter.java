package de.photon.aacadditionpro.util.datastructure.counter;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.concurrent.atomic.AtomicLong;

/**
 * This counter is used for the common use case of "only flag after x consecutive violations".
 */
@EqualsAndHashCode
@ToString
@RequiredArgsConstructor
public class ViolationCounter
{
    private final AtomicLong counter = new AtomicLong();
    private final long limit;

    /**
     * Increments the counter by one and checks if the limit has been reached.
     */
    public boolean incrementAndCheck()
    {
        return counter.incrementAndGet() >= limit;
    }

    /**
     * Decrements the counter by one and checks if the limit has been reached.
     */
    public boolean decrementAndCheck()
    {
        return counter.decrementAndGet() >= limit;
    }

    /**
     * Checks if the limit has been reached.
     */
    public boolean check()
    {
        return counter.get() >= limit;
    }

    public void setToZero()
    {
        counter.set(0);
    }
}
