package de.photon.anticheataddition.util.reflection;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.util.messaging.Log;
import lombok.Value;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

@Value
public class ConstructorReflect
{
    Constructor<?> constructor;

    public Object instance(Object... initObjects)
    {
        try {
            return this.constructor.newInstance(initObjects);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            Log.error("Unable to invoke instance via constructor reflection", e);
        }
        return null;
    }
}
