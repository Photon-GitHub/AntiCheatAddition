package de.photon.anticheataddition.user.data;

import com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class DataMap
{
    private final Map<DataKey.Bool, Boolean> boolMap = Collections.synchronizedMap(new EnumMap<>(DataKey.Bool.class));
    private final Map<DataKey.Int, Integer> intMap = Collections.synchronizedMap(new EnumMap<>(DataKey.Int.class));
    private final Map<DataKey.Long, Long> longMap = Collections.synchronizedMap(new EnumMap<>(DataKey.Long.class));
    private final Map<DataKey.Float, Float> floatMap = Collections.synchronizedMap(new EnumMap<>(DataKey.Float.class));
    private final Map<DataKey.Double, Double> doubleMap = Collections.synchronizedMap(new EnumMap<>(DataKey.Double.class));
    private final Map<DataKey.Count, ViolationCounter> counterMap = Collections.synchronizedMap(new EnumMap<>(DataKey.Count.class));
    private final Map<DataKey.Obj, Object> objectMap = Collections.synchronizedMap(new EnumMap<>(DataKey.Obj.class));

    public boolean getBoolean(DataKey.Bool key)
    {
        return this.boolMap.computeIfAbsent(key, DataKey.Bool::isDefaultValue);
    }

    public int getInt(DataKey.Int key)
    {
        return this.intMap.computeIfAbsent(key, DataKey.Int::getDefaultValue);
    }

    public long getLong(DataKey.Long key)
    {
        return this.longMap.computeIfAbsent(key, DataKey.Long::getDefaultValue);
    }

    public float getFloat(DataKey.Float key)
    {
        return this.floatMap.computeIfAbsent(key, DataKey.Float::getDefaultValue);
    }

    public double getDouble(DataKey.Double key)
    {
        return this.doubleMap.computeIfAbsent(key, DataKey.Double::getDefaultValue);
    }

    public ViolationCounter getCounter(DataKey.Count key)
    {
        return this.counterMap.computeIfAbsent(key, DataKey.Count::createDefaultCounter);
    }

    public Object getObject(DataKey.Obj key)
    {
        return this.objectMap.computeIfAbsent(key, DataKey.Obj::createDefaultObject);
    }

    public void setBoolean(DataKey.Bool key, Boolean b)
    {
        this.boolMap.put(key, b);
    }

    public void setInt(DataKey.Int key, Integer i)
    {
        this.intMap.put(key, i);
    }

    public void setLong(DataKey.Long key, Long l)
    {
        this.longMap.put(key, l);
    }

    public void setFloat(DataKey.Float key, Float f)
    {
        this.floatMap.put(key, f);
    }

    public void setFloat(DataKey.Double key, Double d)
    {
        this.doubleMap.put(key, d);
    }

    public void setObject(DataKey.Obj key, Object o)
    {
        Preconditions.checkArgument(key.getClazz().isAssignableFrom(o.getClass()), "Tried to assign wrong type to key");
        this.objectMap.put(key, o);
    }
}
