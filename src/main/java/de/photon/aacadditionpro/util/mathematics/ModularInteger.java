package de.photon.aacadditionpro.util.mathematics;

public class ModularInteger
{
    private final int mod;
    private final int lastInt;
    private int integer;

    public ModularInteger(int integer, int mod)
    {
        this.mod = mod;
        this.lastInt = mod - 1;
        set(integer);
    }

    public int get()
    {
        return this.integer;
    }

    public ModularInteger set(int integer)
    {
        this.integer = integer;
        this.integer %= mod;
        // Avoid modulo operations as they are slow.
        if (this.integer < 0) this.integer += mod;
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
        // Avoid modulo operations as they are slow.
        if (++this.integer == mod) this.integer = 0;
        return this.integer;
    }

    public int decrementAndGet()
    {
        // Avoid modulo operations as they are slow.
        if (--this.integer < 0) this.integer = this.lastInt;
        return this.integer;
    }

    public int getAndIncrement()
    {
        final int ret = this.integer;
        // Avoid modulo operations as they are slow.
        if (++this.integer == mod) this.integer = 0;
        return ret;
    }

    public int getAndDecrement()
    {
        final int ret = this.integer;
        // Avoid modulo operations as they are slow.
        if (--this.integer < 0) this.integer = this.lastInt;
        return ret;
    }
}
