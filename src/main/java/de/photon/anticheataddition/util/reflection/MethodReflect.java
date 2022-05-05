package de.photon.anticheataddition.util.reflection;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.util.messaging.Log;
import lombok.Value;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

@Value
public class MethodReflect
{
    Method method;

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