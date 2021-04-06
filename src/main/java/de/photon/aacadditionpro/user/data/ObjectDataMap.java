package de.photon.aacadditionpro.user.data;

import com.google.common.base.Preconditions;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class ObjectDataMap
{
    private final Map<Datakey, Object> map = Collections.synchronizedMap(new EnumMap<>(Datakey.class));

    private static void checkType(Datakey key, Class clazz)
    {
        Preconditions.checkArgument(key.getTypeClass().isAssignableFrom(clazz), "Tried to get or insert wrong type for key " + key);
    }

    public void setValue(Datakey key, Object value)
    {
        checkType(key, value.getClass());
        this.map.put(key, value);
    }

    public void clear()
    {
        this.map.clear();
    }

    public Object getValue(Datakey key)
    {
        return this.map.get(key);
    }

    public boolean getBoolean(Datakey key)
    {
        checkType(key, Boolean.class);
        return (boolean) this.getValue(key);
    }

    public byte getByte(Datakey key)
    {
        checkType(key, Byte.class);
        return (byte) this.getValue(key);
    }

    public short getShort(Datakey key)
    {
        checkType(key, Short.class);
        return (short) this.getValue(key);
    }

    public int getInt(Datakey key)
    {
        checkType(key, Integer.class);
        return (int) this.getValue(key);
    }

    public long getLong(Datakey key)
    {
        checkType(key, Long.class);
        return (long) this.getValue(key);
    }

    public float getFloat(Datakey key)
    {
        checkType(key, Float.class);
        return (float) this.getValue(key);
    }

    public double getDouble(Datakey key)
    {
        checkType(key, Double.class);
        return (double) this.getValue(key);
    }

    public char getChar(Datakey key)
    {
        checkType(key, Character.class);
        return (char) this.getValue(key);
    }

    public String getString(Datakey key)
    {
        checkType(key, String.class);
        return (String) this.getValue(key);
    }

    public Material getMaterial(Datakey key)
    {
        checkType(key, Material.class);
        return (Material) this.getValue(key);
    }

    public ItemStack getItemStack(Datakey key)
    {
        checkType(key, ItemStack.class);
        return (ItemStack) this.getValue(key);
    }
}
