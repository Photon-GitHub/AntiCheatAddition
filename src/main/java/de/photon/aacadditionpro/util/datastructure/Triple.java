package de.photon.aacadditionpro.util.datastructure;

import lombok.Value;

import java.util.function.BiFunction;

@Value(staticConstructor = "of")
public class Triple<E, F, G>
{
    E first;
    F second;
    G third;

    public static <E, F, G> Triple<E, F, G> fromPair(E one, F two, BiFunction<E, F, G> mapping)
    {
        return of(one, two, mapping.apply(one, two));
    }

    public static <E, F, G> Triple<E, F, G> fromPair(Pair<E, F> pair, BiFunction<E, F, G> mapping)
    {
        return fromPair(pair.getFirst(), pair.getSecond(), mapping);
    }
}
