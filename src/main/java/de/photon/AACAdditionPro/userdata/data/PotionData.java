package de.photon.AACAdditionPro.userdata.data;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.userdata.Data;
import de.photon.AACAdditionPro.userdata.User;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PotionData extends Data
{
    public PotionData(final User theUser)
    {
        super(false, theUser);
    }

    private PotionEffect getPotionEffect(final PotionEffectType type)
    {
        switch (AACAdditionPro.getInstance().getServerVersion()) {
            case MC188:
                for (final PotionEffect effect : theUser.getPlayer().getActivePotionEffects()) {
                    if (effect.getType().equals(type)) {
                        return effect;
                    }
                }
                return null;
            case MC110:
            case MC111:
            case MC112:
                return theUser.getPlayer().getPotionEffect(type);
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    /**
     * Used to get the Amplifier of a {@link PotionEffect} a player has.
     *
     * @param type The {@link PotionEffectType} of the {@link PotionEffect}
     * @return the amplifier of the {@link PotionEffect} incremented by 1.
     * This is the number the player will see in his inventory (Speed I returns 1).
     * If there is no {@link PotionEffect} which has the right {@link PotionEffectType} -1 will be returned.
     */
    public short getAmplifier(final PotionEffectType type)
    {
        final PotionEffect effect = this.getPotionEffect(type);
        return effect == null ?
               Short.MIN_VALUE :
               (short) (1 + effect.getAmplifier());
    }
}
