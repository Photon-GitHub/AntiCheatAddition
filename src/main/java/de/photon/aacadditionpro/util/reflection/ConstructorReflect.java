package de.photon.aacadditionpro.util.reflection;

import lombok.Getter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ConstructorReflect
{
    @Getter
    private final Constructor constructor;

    ConstructorReflect(Constructor constructor)
    {
        this.constructor = constructor;
    }
    

    public Object instance(Object... initObjects)
    {
        try {
            return this.constructor.newInstance(initObjects);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}
