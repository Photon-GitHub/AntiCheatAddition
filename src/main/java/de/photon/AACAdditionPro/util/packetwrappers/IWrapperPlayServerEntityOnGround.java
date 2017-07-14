package de.photon.AACAdditionPro.util.packetwrappers;

public interface IWrapperPlayServerEntityOnGround extends IWrapperPlayServerEntity
{

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
