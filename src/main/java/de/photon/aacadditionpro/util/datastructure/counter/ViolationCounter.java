package de.photon.aacadditionpro.util.datastructure.counter;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

import java.util.concurrent.atomic.AtomicLong;

/**
 * This counter is used for the common use case of "only flag after x consecutive violations".
 */
@Value
@Getter(value = AccessLevel.NONE)
public class ViolationCounter
{
    AtomicLong counter = new AtomicLong();
    long limit;

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
