package de.photon.anticheataddition.util.datastructure;

/**
 * This defines a generic, immutable pair of values.
 */
public record Pair<E, F>(E first, F second)
{
    public static <E, F> Pair<E, F> of(E first, F second)
    {
        return new Pair<>(first, second);
    }
}
