package de.photon.AACAdditionPro.util.random;

/**
 * Defines an action which is called in a cycle.
 * In a random cycle count it will execute that action.
 */
public abstract class RandomizedAction
{
    private int counter;

    private int min;
    private int boundary;

    private int nextAction;

    /**
     * Constructs a new {@link RandomizedAction}
     *
     * @param min      the minimum count of cycles before the action can trigger again
     * @param boundary the maximum count of cycles that may pass after the minimum count has been exceeded before the action is bound to happen.
     */
    public RandomizedAction(int min, int boundary)
    {
        this.min = min;
        this.boundary = boundary;

        this.resetCycle();
    }

    protected abstract void run();

    public void cycle()
    {
        if (++this.counter >= nextAction)
        {
            this.run();
            this.resetCycle();
        }
    }

    private void resetCycle()
    {
        this.nextAction = RandomUtil.randomBoundaryInt(min, boundary);
        this.counter = 0;
    }
}
