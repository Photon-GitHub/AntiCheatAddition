package de.photon.anticheataddition.util.datastructure;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import lombok.experimental.UtilityClass;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class SetUtil
{
    /**
     * Directly collects a stream to an immutable EnumSet.
     * Guavas {@link Sets#toImmutableEnumSet()} cannot be used as minecraft 1.8's guava is very old.
     */
    public static <E extends Enum<E>> ImmutableSet<E> toImmutableEnumSet(Stream<E> stream, Class<E> enumClass)
    {
        return Sets.immutableEnumSet(stream.collect(toEnumSet(enumClass)));
    }

    /**
     * Special collector to shorten the EnumSet stream collection.
     */
    public static <E extends Enum<E>> Collector<E, ?, EnumSet<E>> toEnumSet(Class<E> enumClass)
    {
        return Collectors.toCollection(() -> EnumSet.noneOf(enumClass));
    }

    /**
     * This computes the difference of two {@link Set}s.
     * Compared to {@link com.google.common.collect.Sets#difference(Set, Set)} this actually computes the {@link Set} and does not return a view, therefore guaranteeing that
     * all computations are only performed once.
     */
    public static <T> Set<T> difference(Set<T> from, Set<T> remove)
    {
        final Set<T> result = new HashSet<>();
        for (T entity : from) {
            if (!remove.contains(entity)) result.add(entity);
        }
        return result;
    }
}
