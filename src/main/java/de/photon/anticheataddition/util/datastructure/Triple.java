package de.photon.anticheataddition.util.datastructure;

import java.util.function.BiFunction;

public record Triple<E, F, G>(E first, F second, G third)
{
    private static <F, E, G> Triple<E, F, G> of(E one, F two, G three)
    {
        return new Triple<>(one, two, three);
    }

    public static <E, F, G> Triple<E, F, G> fromPair(E one, F two, BiFunction<E, F, G> mapping)
    {
        return new Triple<>(one, two, mapping.apply(one, two));
    }

    public static <E, F, G> Triple<E, F, G> fromPair(Pair<E, F> pair, BiFunction<E, F, G> mapping)
    {
        return fromPair(pair.first(), pair.second(), mapping);
    }
}
