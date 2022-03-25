package de.photon.anticheataddition.util.reflection;

import de.photon.anticheataddition.AntiCheatAddition;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.reflect.FieldUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ClassReflect
{
    private final ConcurrentMap<String, FieldReflect> fieldCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ConstructorReflect> constructorCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, MethodReflect> methodCache = new ConcurrentHashMap<>();

    @Getter private final Class<?> clazz;

    public FieldReflect field(String name)
    {
        return this.fieldCache.computeIfAbsent(name, fileName -> new FieldReflect(FieldUtils.getDeclaredField(this.clazz, fileName, true)));
    }

    public ConstructorReflect constructor(Class<?>... classes)
    {
        // Build the key first
        final String cacheKey = Arrays.stream(classes).map(Class::getName).collect(Collectors.joining());

        return this.constructorCache.computeIfAbsent(cacheKey, constructorKey -> {
            // We need to search for the constructor now
            try {
                final Constructor<?> constructor = this.clazz.getConstructor(classes);
                constructor.setAccessible(true);
                return new ConstructorReflect(constructor);
            } catch (NoSuchMethodException e) {
                AntiCheatAddition.getInstance().getLogger().log(Level.SEVERE, "Unable to find method via reflection", e);
            }
            return null;
        });
    }

    public MethodReflect method(String name)
    {
        return this.methodCache.computeIfAbsent(name, methodName -> {
            for (Method method : this.clazz.getDeclaredMethods()) {
                // We take the first method with the name
                if (name.equals(method.getName())) {
                    method.setAccessible(true);
                    return new MethodReflect(method);
                }
            }
            return null;
        });
    }
}
