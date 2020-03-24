package de.photon.aacadditionpro.user;

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
}
