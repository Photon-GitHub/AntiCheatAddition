package de.photon.anticheataddition.util.reflection;

import de.photon.anticheataddition.AntiCheatAddition;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

@RequiredArgsConstructor
public class MethodReflect
{
    @Getter private final Method method;

    public Object invoke(Object obj, Object... args)
    {
        try {
            return this.method.invoke(obj, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            AntiCheatAddition.getInstance().getLogger().log(Level.SEVERE, "Unable to invoke field via reflection", e);
        }

        return null;
    }
}