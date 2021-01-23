package de.photon.aacadditionproold.util.reflection;

import de.photon.aacadditionproold.AACAdditionPro;
import lombok.Getter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

public class MethodReflect
{
    @Getter
    private final Method method;

    MethodReflect(Method method)
    {
        this.method = method;
    }

    public Object invoke(Object obj, Object... args)
    {
        try {
            return this.method.invoke(obj, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            AACAdditionPro.getInstance().getLogger().log(Level.SEVERE, "Unable to invoke field via reflection", e);
        }

        return null;
    }
}