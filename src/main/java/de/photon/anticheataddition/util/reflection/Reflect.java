package de.photon.anticheataddition.util.reflection;

import de.photon.anticheataddition.AntiCheatAddition;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * @author geNAZt
 * @version 1.0
 */
@UtilityClass
public class Reflect
{
    private static final String BUKKIT_VERSION_NUMBER = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
    private static final Map<String, ClassReflect> REFLECTION_CACHE = new ConcurrentHashMap<>();

    public static ClassReflect from(Class<?> clazz)
    {
        // computeIfAbsent automatically puts the new value in the REFLECTION_CACHE.
        return REFLECTION_CACHE.computeIfAbsent(clazz.getName(), key -> new ClassReflect(clazz));
    }

    public static ClassReflect from(String classPath)
    {
        try {
            return from(Reflect.class.getClassLoader().loadClass(classPath));
        } catch (ClassNotFoundException e) {
            AntiCheatAddition.getInstance().getLogger().log(Level.SEVERE, "Unable to reflect class from path.", e);
        }

        return null;
    }

    public static ClassReflect fromNMS(String classPath)
    {
        return from(("net.minecraft.server." + BUKKIT_VERSION_NUMBER) + "." + classPath);
    }

    public static ClassReflect fromOBC(String classPath)
    {
        return from(("org.bukkit.craftbukkit." + BUKKIT_VERSION_NUMBER) + "." + classPath);
    }
}
