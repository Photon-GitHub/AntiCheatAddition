package de.photon.aacadditionpro.user.data;

import com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class DataMap
{
    private final Map<DataKey.BooleanKey, Boolean> boolMap = Collections.synchronizedMap(new EnumMap<>(DataKey.BooleanKey.class));
    private final Map<DataKey.IntegerKey, Integer> intMap = Collections.synchronizedMap(new EnumMap<>(DataKey.IntegerKey.class));
    private final Map<DataKey.LongKey, Long> longMap = Collections.synchronizedMap(new EnumMap<>(DataKey.LongKey.class));
    private final Map<DataKey.FloatKey, Float> floatMap = Collections.synchronizedMap(new EnumMap<>(DataKey.FloatKey.class));
    private final Map<DataKey.DoubleKey, Double> doubleMap = Collections.synchronizedMap(new EnumMap<>(DataKey.DoubleKey.class));
    private final Map<DataKey.ObjectKey, Object> objectMap = Collections.synchronizedMap(new EnumMap<>(DataKey.ObjectKey.class));

    public boolean getBoolean(DataKey.BooleanKey key)
    {
        return this.boolMap.getOrDefault(key, key.getDefaultValue());
    }

    public int getInt(DataKey.IntegerKey key)
    {
        return this.intMap.getOrDefault(key, key.getDefaultValue());
    }

    public long getLong(DataKey.LongKey key)
    {
        return this.longMap.getOrDefault(key, key.getDefaultValue());
    }

    public float getFloat(DataKey.FloatKey key)
    {
        return this.floatMap.getOrDefault(key, key.getDefaultValue());
    }

    public double getDouble(DataKey.DoubleKey key)
    {
        return this.doubleMap.getOrDefault(key, key.getDefaultValue());
    }

    public Object getObject(DataKey.ObjectKey key)
    {
        return this.objectMap.getOrDefault(key, key.getDefaultValue());
    }

    public void setBoolean(DataKey.BooleanKey key, Boolean b)
    {
        this.boolMap.put(key, b);
    }

    public void setInt(DataKey.IntegerKey key, Integer i)
    {
        this.intMap.put(key, i);
    }

    public void setLong(DataKey.LongKey key, Long l)
    {
        this.longMap.put(key, l);
    }

    public void setFloat(DataKey.FloatKey key, Float f)
    {
        this.floatMap.put(key, f);
    }

    public void setFloat(DataKey.DoubleKey key, Double d)
    {
        this.doubleMap.put(key, d);
    }

    public void setObject(DataKey.ObjectKey key, Object o)
    {
        Preconditions.checkArgument(key.getClazz().isAssignableFrom(o.getClass()), "Tried to assign wrong type to key");
        this.objectMap.put(key, o);
    }
}
