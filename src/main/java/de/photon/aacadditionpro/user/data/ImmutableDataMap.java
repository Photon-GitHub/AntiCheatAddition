package de.photon.aacadditionpro.user.data;

public interface ImmutableDataMap<T extends Enum<T>, V>
{
    /**
     * Gets a value from the map.
     *
     * @return the value associated with the key.
     */
    V getValue(T key);
}
