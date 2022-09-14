package de.photon.anticheataddition.util.datastructure;

import java.util.function.BiFunction;

public record Triple<E, F, G>(E first, F second, G third)
{
    public static <E, F, G> Triple<E, F, G> fromPair(E one, F two, BiFunction<E, F, G> mapping)
    {
        return new Triple<>(one, two, mapping.apply(one, two));
    }

    public static <E, F, G> Triple<E, F, G> fromPair(Pair<E, F> pair, BiFunction<E, F, G> mapping)
    {
        return fromPair(pair.first(), pair.second(), mapping);
    }
}
