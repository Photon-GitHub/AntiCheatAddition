package de.photon.aacadditionpro.util.minecraft.tps;

public interface TPSProvider
{
    TPSProvider INSTANCE = new CCTPSProvider();

    /**
     * Gets the current TPS of the server.
     */
    double getTPS();

    /**
     * Checks if the current TPS are higher than min.
     */
    default boolean atLeastTPS(double min)
    {
        return this.getTPS() >= min;
    }
}
