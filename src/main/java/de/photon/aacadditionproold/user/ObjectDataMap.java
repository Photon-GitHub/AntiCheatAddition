package de.photon.aacadditionproold.user;

import com.google.common.base.Preconditions;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class ObjectDataMap<T extends Enum<T>> extends DataMap<T, Object>
{
    /**
     * This {@link Predicate} is supposed to be used to verify the class type of the data stored in this map.
     */
    private final BiPredicate<T, Object> checkType;

    public ObjectDataMap(Class<T> enumeration, BiPredicate<T, Object> checkType)
    {
        super(enumeration);
        this.checkType = checkType;
    }

    @Override
    public void setValue(T key, Object value)
    {
        Preconditions.checkArgument(checkType.test(key, value), "Tried to insert wrong type for key " + key);
        super.setValue(key, value);
    }

    // Casting for primitives:
    public boolean getBoolean(T key)
    {
        Preconditions.checkArgument(checkType.test(key, true), "Tried to get wrong type for key " + key);
        return (boolean) this.getValue(key);
    }

    public byte getByte(T key)
    {
        Preconditions.checkArgument(checkType.test(key, Byte.MAX_VALUE), "Tried to get wrong type for key " + key);
        return (byte) this.getValue(key);
    }

    public short getShort(T key)
    {
        Preconditions.checkArgument(checkType.test(key, Short.MAX_VALUE), "Tried to get wrong type for key " + key);
        return (short) this.getValue(key);
    }

    public int getInt(T key)
    {
        Preconditions.checkArgument(checkType.test(key, Integer.MAX_VALUE), "Tried to get wrong type for key " + key);
        return (int) this.getValue(key);
    }

    public long getLong(T key)
    {
        Preconditions.checkArgument(checkType.test(key, Long.MAX_VALUE), "Tried to get wrong type for key " + key);
        return (long) this.getValue(key);
    }

    public float getFloat(T key)
    {
        Preconditions.checkArgument(checkType.test(key, Float.MAX_VALUE), "Tried to get wrong type for key " + key);
        return (float) this.getValue(key);
    }

    public double getDouble(T key)
    {
        Preconditions.checkArgument(checkType.test(key, Double.MAX_VALUE), "Tried to get wrong type for key " + key);
        return (double) this.getValue(key);
    }
}
