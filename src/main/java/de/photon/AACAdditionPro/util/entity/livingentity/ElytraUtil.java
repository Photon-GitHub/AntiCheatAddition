package de.photon.AACAdditionPro.util.entity.livingentity;

import de.photon.AACAdditionPro.ServerVersion;
import org.bukkit.entity.LivingEntity;

/**
 * Contains util methods regarding the Elytra.
 */
public final class ElytraUtil
{
    public static boolean isFlyingWithElytra(final LivingEntity livingEntity)
    {
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                return false;
            case MC111:
            case MC112:
                return livingEntity.isGliding();
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }
}
