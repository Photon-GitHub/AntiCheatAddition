package de.photon.aacadditionpro.util.datastructure;

import lombok.Value;

import java.util.function.BiFunction;

/**
 * This defines a generic, immutable pair of values.
 */
@Value(staticConstructor = "of")
public class Pair<E, F>
{
    E first;
    F second;

    public static <E, F, T> T map(E e, F f, BiFunction<E, F, T> mapping)
    {
        return mapping.apply(e, f);
    }

    public <T> T map(BiFunction<E, F, T> mapping)
    {
        return mapping.apply(first, second);
    }
}
