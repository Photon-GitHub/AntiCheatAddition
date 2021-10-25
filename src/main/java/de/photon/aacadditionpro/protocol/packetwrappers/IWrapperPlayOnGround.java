package de.photon.aacadditionpro.protocol.packetwrappers;

public interface IWrapperPlayOnGround extends IWrapperPlay
{
    /**
     * Retrieve On Ground.
     * <p>
     * Notes: true if the client is on the ground, False otherwise
     *
     * @return The current On Ground
     */
    default boolean getOnGround()
    {
        return this.getHandle().getBooleans().read(0);
    }

    /**
     * Set On Ground.
     *
     * @param value - new value.
     */
    default void setOnGround(final boolean value)
    {
        this.getHandle().getBooleans().write(0, value);
    }
}
