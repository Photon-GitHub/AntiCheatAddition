package de.photon.AACAdditionPro.util.entity;


import de.photon.AACAdditionPro.ServerVersion;
import de.photon.AACAdditionPro.util.exceptions.UnknownMinecraftVersion;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Contains util methods regarding the {@link org.bukkit.potion.PotionEffect}s and {@link org.bukkit.potion.PotionEffectType}s.
 */
public final class PotionUtil
{
    private PotionUtil() {}

    /**
     * Gets a {@link PotionEffect} of a {@link LivingEntity}.
     *
     * @param livingEntity the {@link LivingEntity} which should be tested
     * @param type         the {@link PotionEffectType} which should be tested for
     *
     * @return the {@link PotionEffect} with the provided {@link PotionEffectType} or null if the {@link LivingEntity}
     * doesn't have such a {@link PotionEffect}.
     */
    public static PotionEffect getPotionEffect(final LivingEntity livingEntity, final PotionEffectType type)
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                for (final PotionEffect effect : livingEntity.getActivePotionEffects()) {
                    if (effect.getType().equals(type)) {
                        return effect;
                    }
                }
                return null;
            case MC113:
            case MC114:
            case MC115:
                return livingEntity.getPotionEffect(type);
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    /**
     * Used to get the Amplifier of a {@link PotionEffect}.
     *
     * @param effect the effect which should be tested.
     *
     * @return the amplifier of the {@link PotionEffect} or null if the player doesn't have it.
     */
    public static Integer getAmplifier(final PotionEffect effect)
    {
        return effect == null ?
               null :
               effect.getAmplifier();
    }
}
