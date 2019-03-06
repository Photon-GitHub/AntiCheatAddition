package de.photon.AACAdditionPro.util.reflection;

import lombok.Getter;

import java.lang.reflect.Field;

public class FieldReflect
{
    @Getter
    private final Field field;

    FieldReflect(Field field)
    {
        this.field = field;
    }

    public TempValueReflect from(Object obj)
    {
        return new TempValueReflect(field, obj);
    }
}
