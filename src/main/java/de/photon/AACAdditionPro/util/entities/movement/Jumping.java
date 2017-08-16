package de.photon.AACAdditionPro.util.entities.movement;

public final class Jumping
{
    /**
     * Client-Copy for exact values.
     * This gets the y-Motion of a {@link org.bukkit.entity.Player} for every JumpBoost effect.
     *
     * @param amplifier the amplifier of the Jump_Boost effect. If no effect should be applied this should be null
     */
    public static double getJumpYMotion(final Integer amplifier)
    {
        double motionY = (double) 0.42F;

        // If the amplifier is null no effect should be applied.
        if (amplifier != null) {
            // Increase amplifier by one as e.g. amplifier 0 makes up Speed I
            motionY += (double) ((float) ((amplifier + 1) * 0.1F));
        }

        return motionY;
    }
}
