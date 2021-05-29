package de.photon.aacadditionpro.util.datastructure;

import lombok.Value;

@Value
public class ImmutablePair<E, F>
{
    E first;
    F second;
}
