package de.photon.AACAdditionPro.util.storage.management;

public abstract class DoubleBuffer extends Buffer<Double>
{
    public DoubleBuffer(int buffer_size)
    {
        super(buffer_size);
    }

    /**
     * Adds all the {@link Double}s in this {@link Buffer}.
     *
     * @return the sum of all elements in this {@link Buffer}.
     */
    public double sum()
    {
        double result = 0D;
        for (double d : this) {
            result += d;
        }
        return result;
    }

    /**
     * @return the average of all {@link Double}s in this {@link Buffer}
     */
    public double average()
    {
        return this.sum() / this.size();
    }
}
