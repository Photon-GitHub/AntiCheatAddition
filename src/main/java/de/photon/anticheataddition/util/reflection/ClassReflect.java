package de.photon.anticheataddition.util.reflection;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.util.messaging.Log;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public final class ClassReflect
{
    private final ConcurrentMap<String, FieldReflect> fieldCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ConstructorReflect> constructorCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, MethodReflect> methodCache = new ConcurrentHashMap<>();

    @Getter private final Class<?> clazz;

    public static Field getDeclaredField(Class cls, String fieldName)
    {
        Preconditions.checkNotNull(cls, "The class must not be null");
        Preconditions.checkNotNull(fieldName, "The field name must not be null");

        try {
            // only consider the specified class by using getDeclaredField()
            final Field field = cls.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException ignored) {
        }
        return null;
    }

    public FieldReflect field(String name)
    {
        return this.fieldCache.computeIfAbsent(name, fileName -> new FieldReflect(getDeclaredField(this.clazz, fileName)));
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
                Log.error("Unable to find method via reflection", e);
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
