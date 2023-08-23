package de.photon.anticheataddition.user.data;

import com.google.common.base.Preconditions;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongUnaryOperator;

public final class ViolationCounter
{
    private static final LongUnaryOperator DECREMENT_ABOVE_ZERO_FUNCTION = l -> (l > 0 ? l - 1 : 0);

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
    public boolean greaterOrEqualToThreshold()
    {
        return this.counter.get() >= threshold;
    }

    /**
     * Checks if the counter is smaller than the threshold.
     */
    public boolean smallerThanThreshold()
    {
        return this.counter.get() < threshold;
    }

    /**
     * Checks if the counter, incremented by 1, is greater or equal to the threshold.
     */
    public boolean incrementCompareThreshold()
    {
        return this.counter.incrementAndGet() >= threshold;
    }

    /**
     * Checks if the counter, incremented by amount, is greater or equal to the threshold.
     */
    public boolean incrementCompareThreshold(int amount)
    {
        return this.counter.addAndGet(amount) >= threshold;
    }

    /**
     * This method calls {@link #incrementCompareThreshold()} if the condition is true, else {@link #decrementAboveZero()}.
     *
     * @return true if the condition is true and {@link #incrementCompareThreshold()} also returns true, else false.
     */
    public boolean conditionallyIncDec(boolean condition)
    {
        if (condition) return this.incrementCompareThreshold();
        else {
            this.decrementAboveZero();
            return false;
        }
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
     * Decrements the counter by 1 unless the counter is zero or negative.
     */
    public void decrementAboveZero()
    {
        this.counter.updateAndGet(DECREMENT_ABOVE_ZERO_FUNCTION);
    }

    /**
     * Sets the counter to 0.
     */
    public void setToZero()
    {
        this.counter.set(0);
    }
}
