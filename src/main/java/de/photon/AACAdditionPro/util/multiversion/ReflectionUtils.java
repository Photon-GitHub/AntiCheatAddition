package de.photon.AACAdditionPro.util.multiversion;

import de.photon.AACAdditionPro.AACAdditionPro;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;

public final class ReflectionUtils
{
    /**
     * Used to get the version {@link String} that is necessary for net.minecraft.server reflection
     *
     * @return e.g. v1_11_R1
     */
    public static String getVersionString()
    {
        return Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
    }

    /**
     * Loads a {@link Class}, gets a {@link Method} from it and makes it accessible.
     *
     * @param path       the class-path of the class with the desired method
     * @param methodName the name of the method that should be returned
     * @return the {@link Method} that refers to the parameters or null if no {@link Method} was found.
     * @throws ClassNotFoundException if there is no class referring to the path
     * @throws NoSuchMethodException  if there is no method referring to the methodName
     */
    public static Method getMethodFromPath(final String path, final String methodName, final boolean accessible) throws ClassNotFoundException, NoSuchMethodException
    {
        final Method method = loadClassFromPath(path).getDeclaredMethod(methodName);
        method.setAccessible(accessible);
        return method;
    }

    /**
     * Loads a {@link Class}
     *
     * @param path the class-path of the class which should be loaded
     * @return the {@link Class} that refers to the parameters or null if no {@link Class} was found.
     * @throws ClassNotFoundException if there is no class referring to the path
     */
    public static Class loadClassFromPath(final String path) throws ClassNotFoundException
    {
        return AACAdditionPro.getInstance().getClass().getClassLoader().loadClass(path);
    }
}
