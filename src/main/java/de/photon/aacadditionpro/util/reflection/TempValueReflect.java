package de.photon.aacadditionpro.util.reflection;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

/**
 * @author geNAZt
 * @version 1.0
 */
public class TempValueReflect
{
    private final Object obj;
    private final Field field;

    TempValueReflect(Field field, Object obj)
    {
        this.field = field;
        this.obj = obj;
    }

    public <T> T as(Class<T> clazz)
    {
        try {
            return (T) this.field.get(obj);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    public byte[] asBytes()
    {
        try {
            return (byte[]) this.field.get(obj);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return new byte[0];
    }

    public double asDouble()
    {
        try {
            return this.field.getDouble(obj);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public <T> List<T> asList(Class<T> clazz)
    {
        try {
            return (List<T>) this.field.get(obj);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    public <T> Set<T> asSet(Class<T> clazz)
    {
        try {
            return (Set<T>) this.field.get(obj);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }
}
