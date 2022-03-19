package de.photon.anticheataddition.util.datastructure;

import lombok.Value;

/**
 * This defines a generic, immutable pair of values.
 */
@Value(staticConstructor = "of")
public class Pair<E, F>
{
    E first;
    F second;
}
