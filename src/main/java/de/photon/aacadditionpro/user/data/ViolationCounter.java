package de.photon.aacadditionpro.user.data;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
public class ViolationCounter
{
    private final AtomicLong counter = new AtomicLong(0);
    private final long threshold;

    /**
     * Gets the current count
     */
    public long getCounter()
    {
        return this.counter.get();
    }

    /**
     * Checks if the counter is greater or equal to the threshold.
     */
    public boolean compareThreshold()
    {
        return this.counter.get() >= threshold;
    }

    /**
     * Checks if the counter, incremented by 1, is greater or equal to the threshold.
     */
    public boolean incrementCompareThreshold()
    {
        // getAndIncrement due to the config comments of "x violations are fine"
        return this.counter.getAndIncrement() >= threshold;
    }

    /**
     * Decrements the counter by 1.
     */
    public void decrement()
    {
        this.counter.getAndDecrement();
    }

    /**
     * Sets the counter to 0.
     */
    public void setToZero()
    {
        this.counter.set(0);
    }
}
