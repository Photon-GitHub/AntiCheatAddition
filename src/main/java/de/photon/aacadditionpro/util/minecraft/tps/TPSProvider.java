package de.photon.aacadditionpro.util.minecraft.tps;

public interface TPSProvider
{
    TPSProvider INSTANCE = new CCTPSProvider();

    /**
     * Gets the current TPS of the server.
     */
    double getTPS();
}
