package de.photon.aacadditionpro.util.datastructure;

import lombok.Value;

@Value(staticConstructor = "of")
public class ImmutablePair<E, F>
{
    E first;
    F second;
}
