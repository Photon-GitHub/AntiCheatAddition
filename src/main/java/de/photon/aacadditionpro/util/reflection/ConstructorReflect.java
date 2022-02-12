package de.photon.aacadditionpro.util.reflection;

import de.photon.aacadditionpro.AACAdditionPro;
import lombok.Getter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

public class ConstructorReflect
{
    @Getter
    private final Constructor<?> constructor;

    ConstructorReflect(Constructor<?> constructor)
    {
        this.constructor = constructor;
    }


    public Object instance(Object... initObjects)
    {
        try {
            return this.constructor.newInstance(initObjects);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            AACAdditionPro.getInstance().getLogger().log(Level.SEVERE, "Unable to invoke instance via constructor reflection", e);
        }
        return null;
    }
}
