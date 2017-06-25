package de.photon.AACAdditionPro.util.packetwrappers;

import com.comphenix.protocol.events.PacketContainer;

public interface IWrapperPlayServerEntityOnGround
{

    PacketContainer getHandle();

    /**
     * Retrieve On Ground.
     *
     * @return The current On Ground
     */
    default boolean getOnGround()
    {
        return getHandle().getBooleans().read(0);
    }

    /**
     * Set On Ground.
     *
     * @param value - new value.
     */
    default void setOnGround(boolean value)
    {
        getHandle().getBooleans().write(0, value);
    }

}
