package de.photon.aacadditionpro.user;

import com.google.common.base.Preconditions;

import java.util.function.Predicate;

public class LongDataMap<T extends Enum<T>> extends DataMap<T, Long>
{
    public static final String VALUE_REQUIRED = "Key is a timestamp, but should be a value.";
    public static final String TIMESTAMP_REQUIRED = "Key is a value, but should be a timestamp.";

    private final Predicate<T> isTimeStamp;

    public LongDataMap(Class<T> enumeration, Predicate<T> isTimeStamp)
    {
        super(enumeration);
        this.isTimeStamp = isTimeStamp;
    }

    @Override
    public Long getValue(T key)
    {
        Preconditions.checkArgument(!this.isTimeStamp.test(key), VALUE_REQUIRED);
        return super.getValue(key);
    }

    @Override
    public void setValue(T key, Long value)
    {
        Preconditions.checkArgument(!this.isTimeStamp.test(key), VALUE_REQUIRED);
        super.setValue(key, value);
    }

    public void addValue(T key, long add)
    {
        Preconditions.checkArgument(!this.isTimeStamp.test(key), VALUE_REQUIRED);
        this.longMap.put(key, this.getValue(key) + add);
    }

    public void subtractValue(T key, long subtract)
    {
        Preconditions.checkArgument(!this.isTimeStamp.test(key), VALUE_REQUIRED);
        this.longMap.put(key, this.getValue(key) - subtract);
    }

    public void multiplyValue(T key, long multiply)
    {
        Preconditions.checkArgument(!this.isTimeStamp.test(key), VALUE_REQUIRED);
        this.longMap.put(key, this.getValue(key) * multiply);
    }


    public Long getTimeStamp(T key)
    {
        Preconditions.checkArgument(this.isTimeStamp.test(key), TIMESTAMP_REQUIRED);
        return this.longMap.get(key);
    }

    public Long passedTime(T key)
    {
        Preconditions.checkArgument(this.isTimeStamp.test(key), TIMESTAMP_REQUIRED);
        return System.currentTimeMillis() - this.longMap.get(key);
    }

    public void updateTimeStamp(T key)
    {
        Preconditions.checkArgument(this.isTimeStamp.test(key), TIMESTAMP_REQUIRED);
        this.longMap.put(key, System.currentTimeMillis());
    }

    public void nullifyTimeStamp(T key)
    {
        Preconditions.checkArgument(this.isTimeStamp.test(key), TIMESTAMP_REQUIRED);
        this.longMap.put(key, 0L);
    }

    public boolean recentlyUpdated(T key, long time)
    {
        return this.passedTime(key) <= time;
    }
}
