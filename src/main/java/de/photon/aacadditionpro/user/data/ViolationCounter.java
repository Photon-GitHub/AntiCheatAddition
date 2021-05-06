package de.photon.aacadditionpro.user.data;

import com.google.common.base.Preconditions;

import java.util.concurrent.atomic.AtomicLong;

public class ViolationCounter
{
    private final AtomicLong counter = new AtomicLong(0);
    private final long threshold;

    public ViolationCounter(long threshold)
    {
        Preconditions.checkArgument(threshold >= 0, "ViolationCounter threshold cannot be smaller than 0.");
        this.threshold = threshold;
    }

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
     * Increments the counter by 1.
     */
    public void increment()
    {
        this.counter.getAndIncrement();
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
