package de.photon.AACAdditionPro.util.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author geNAZt
 * @version 1.0
 */
public class ConstructorReflect {

    private final Constructor constructor;

    ConstructorReflect( Constructor constructor )
    {
        this.constructor = constructor;
    }


    public Object instance( Object ... initObjects )
    {
        try {
            return this.constructor.newInstance( initObjects );
        } catch ( InstantiationException | IllegalAccessException | InvocationTargetException e ) {
            e.printStackTrace();
        }

        return null;
    }

}
