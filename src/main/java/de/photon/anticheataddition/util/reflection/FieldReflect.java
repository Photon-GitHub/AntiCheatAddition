package de.photon.anticheataddition.util.reflection;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;

@RequiredArgsConstructor
public class FieldReflect
{
    @Getter private final Field field;

    public TempValueReflect from(Object obj)
    {
        return new TempValueReflect(field, obj);
    }
}
