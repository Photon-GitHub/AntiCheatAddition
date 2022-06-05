package de.photon.anticheataddition.util.reflection;

import de.photon.anticheataddition.util.messaging.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public record MethodReflect(Method method)
{
    public Object invoke(Object obj, Object... args)
    {
        try {
            return this.method.invoke(obj, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            Log.error("Unable to invoke field via reflection", e);
        }

        return null;
    }
}