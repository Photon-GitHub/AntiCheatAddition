package de.photon.anticheataddition.util.mathematics;

import com.google.common.base.Preconditions;
import lombok.EqualsAndHashCode;

/**
 * A class able of calculations mod n.
 * It provides static methods to directly work on integers, but also allows to be used as a class to for easier handling.
 */
@EqualsAndHashCode
public final class ModularInteger
{
    private final int mod;
    @EqualsAndHashCode.Exclude private final int lastInt;
    private int integer;

    public ModularInteger(int integer, int mod)
    {
        Preconditions.checkArgument(mod >= 2, "Tried to create a modular integer with mod below 2.");
        this.mod = mod;
        this.lastInt = mod - 1;
        set(integer);
    }

    /**
     * A general calculation of what integer should be after mod n.
     * The method will handle negative values automatically and will correctly assign them to n - |value|.
     */
    public static int set(int integer, int mod)
    {
        integer %= mod;
        return integer < 0 ? integer + mod : integer;
    }

    /**
     * Fast incrementation mod n that will avoid a modulo operation.
     * <p>
     * This method assumes that integer is already a valid representation of x mod n and is therefore neither negative
     * nor greater or equal to mod.
     */
    public static int increment(int integer, int mod)
    {
        return ++integer == mod ? 0 : integer;
    }

    /**
     * Fast decrementation mod n that will avoid a modulo operation.
     * <p>
     * This method assumes that integer is already a valid representation of x mod n and is therefore neither negative
     * nor greater or equal to mod.
     */
    public static int decrement(int integer, int mod)
    {
        return --integer < 0 ? mod - 1 : integer;
    }

    /**
     * Gets the value of the underlying integer.
     */
    public int get()
    {
        return this.integer;
    }

    /**
     * Sets the {@link ModularInteger} to a certain value.
     * If the value is not in the range of 0 to n, this method will correctly assign the value of x mod n.
     */
    public ModularInteger set(int integer)
    {
        this.integer = set(integer, this.mod);
        return this;
    }

    /**
     * Fast method to set the {@link ModularInteger} to 0.
     */
    public ModularInteger setToZero()
    {
        this.integer = 0;
        return this;
    }

    public ModularInteger add(int integer)
    {
        return set(this.integer + integer);
    }

    public ModularInteger sub(int integer)
    {
        return set(this.integer - integer);
    }

    public ModularInteger mul(int integer)
    {
        return set(this.integer * integer);
    }

    public ModularInteger increment()
    {
        // Avoid modulo operations as they are slow.
        if (++this.integer == mod) this.integer = 0;
        return this;
    }

    public ModularInteger decrement()
    {
        // Avoid modulo operations as they are slow.
        if (--this.integer < 0) this.integer = this.lastInt;
        return this;
    }

    public int incrementAndGet()
    {
        return increment().get();
    }

    public int decrementAndGet()
    {
        return decrement().get();
    }

    public int getAndIncrement()
    {
        final int ret = get();
        increment();
        return ret;
    }

    public int getAndDecrement()
    {
        final int ret = this.integer;
        decrement();
        return ret;
    }

    /**
     * Creates a copy of the {@link ModularInteger}.
     */
    public ModularInteger copy()
    {
        return new ModularInteger(this.integer, this.mod);
    }
}
