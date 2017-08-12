package de.photon.AACAdditionPro.util.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author geNAZt
 * @version 1.0
 */
public class MethodReflect {

    private final Method method;

    MethodReflect( Method method )
    {
        this.method = method;
    }

    public Object invoke( Object obj, Object ... args )
    {
        try {
            return this.method.invoke( obj, args );
        } catch ( IllegalAccessException | InvocationTargetException e ) {
            e.printStackTrace();
        }

        return null;
    }

}
