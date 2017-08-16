package de.photon.AACAdditionPro.util.reflection;

import de.photon.AACAdditionPro.util.multiversion.ReflectionUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author geNAZt
 * @version 1.0
 */
public class Reflect {

    private static final Map<String, ClassReflect> REFLECTION_CACHE = new ConcurrentHashMap<String, ClassReflect>();

    public static ClassReflect from( Class clazz )
    {
        ClassReflect classReflect = REFLECTION_CACHE.get( clazz.getName() );
        if ( classReflect == null ) {
            classReflect = new ClassReflect( clazz );
            REFLECTION_CACHE.put( clazz.getName(), classReflect );
        }

        return classReflect;
    }

    public static ClassReflect from( String classPath )
    {
        try {
            return from( Reflect.class.getClassLoader().loadClass( classPath ) );
        } catch ( ClassNotFoundException e ) {
            e.printStackTrace();
        }

        return null;
    }

    public static ClassReflect fromNMS(String classPath) {
        return from(("net.minecraft.server." + ReflectionUtils.getVersionString()) + "." + classPath);
    }

    public static ClassReflect fromOBC(String classPath) {
        return from(("org.bukkit.craftbukkit." + ReflectionUtils.getVersionString()) + "." + classPath);
    }
}
