package de.photon.anticheataddition.util.minecraft.movement;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public enum Movement
{
    PLAYER(-0.08),
    PLAYER_SLOW_FALLING(-0.01),
    FALLING_ITEMS_BLOCKS(-0.04),
    PROJECTILES(-0.03);

    /**
     * The gravitation that is applied to that type of entity
     */
    private final double gravitationPerTick;

    Movement(double gravitationPerTick)
    {
        this.gravitationPerTick = gravitationPerTick;
    }

    /**
     * This applies the drag of Minecraft's gravitation system (*= 0.98) to a {@link Vector}
     *
     * @param input the input vector (will not be cloned)
     */
    private static Vector applyAirResistance(@NotNull Vector input)
    {
        return input.multiply((double) 0.98F);
    }

    /**
     * This applies the gravitation of a specific type to a {@link Vector}
     *
     * @param input the input vector (will not be cloned)
     */
    private Vector applyGravitation(@NotNull Vector input)
    {
        return input.setY(input.getY() + this.gravitationPerTick);
    }

    /**
     * This applies the gravitation and the air resistance of a specific type to a {@link Vector}
     *
     * @param input the input vector (will not be cloned)
     */
    public Vector applyGravitationAndAirResistance(@NotNull Vector input)
    {
        return applyAirResistance(applyGravitation(input));
    }

    /**
     * Client-Copy for exact values.
     * This gets the y-Motion of a {@link org.bukkit.entity.Player} for every JumpBoost effect.
     *
     * @param amplifier the amplifier of the Jump_Boost effect. If no effect should be applied this should be null
     */
    @SuppressWarnings({"RedundantCast"})
    public double getJumpYMotion(final Integer amplifier)
    {
        if (this != Movement.PLAYER && this != Movement.PLAYER_SLOW_FALLING) throw new UnsupportedOperationException("Movement type does not support jump y motion calculation.");

        double motionY = (double) 0.42F;

        // If the amplifier is null no effect should be applied.
        if (amplifier != null) {
            // Increase amplifier by one as e.g. amplifier 0 makes up JumpBoost I
            motionY += (double) ((float) ((amplifier + 1) * 0.1F));
        }
        return motionY;
    }

    public static MovementXZSimulator getSpeedSimulator() {
        return MovementXZSimulator.INSTANCE;
    }
}
