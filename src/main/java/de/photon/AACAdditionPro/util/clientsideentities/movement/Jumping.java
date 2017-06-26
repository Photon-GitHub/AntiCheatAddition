package de.photon.AACAdditionPro.util.clientsideentities.movement;

public final class Jumping
{
    /**
     * Client-Copy for exact values.
     * This gets the y-Motion of a {@link org.bukkit.entity.Player} for every JumpBoost effect.
     *
     * @param amplifier the amplifier of the Jump_Boost effect. If no effect should be applied this should be Short.MIN_VALUE
     */
    public static double getJumpYMotion(final short amplifier)
    {
        double motionY = (double) 0.42F;

        // If the amplifier is Short.MIN_VALUE no effect should be applied.
        if (amplifier != Short.MIN_VALUE) {
            motionY += (double) ((float) (amplifier * 0.1F));
        }

        return motionY;
    }
}
