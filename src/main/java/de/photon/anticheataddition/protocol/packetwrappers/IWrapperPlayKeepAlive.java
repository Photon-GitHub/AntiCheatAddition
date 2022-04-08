package de.photon.anticheataddition.protocol.packetwrappers;

import de.photon.anticheataddition.ServerVersion;

public interface IWrapperPlayKeepAlive extends IWrapperPlay
{
    /**
     * Retrieve Keep Alive ID.
     *
     * @return The current Keep Alive ID
     */
    default long getKeepAliveId()
    {
        return ServerVersion.is18() ? (long) getHandle().getIntegers().read(0) : getHandle().getLongs().read(0);
    }

    /**
     * Set Keep Alive ID.
     *
     * @param value - new value.
     */
    default void setKeepAliveId(long value)
    {
        if (ServerVersion.is18()) getHandle().getIntegers().write(0, (int) value);
        else getHandle().getLongs().write(0, value);
    }
}
