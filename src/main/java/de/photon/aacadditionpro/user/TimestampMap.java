package de.photon.aacadditionpro.user;

public class TimestampMap<T extends Enum<T>> extends DataMap<T, Long>
{
    public TimestampMap(Class<T> enumeration)
    {
        super(enumeration);
    }

    public Long getTimeStamp(T key)
    {
        return this.map.get(key);
    }

    public Long passedTime(T key)
    {
        return System.currentTimeMillis() - this.map.get(key);
    }

    public void updateTimeStamp(T key)
    {
        this.map.put(key, System.currentTimeMillis());
    }

    public void nullifyTimeStamp(T key)
    {
        this.map.put(key, 0L);
    }

    public boolean recentlyUpdated(T key, long time)
    {
        return this.passedTime(key) <= time;
    }
}
