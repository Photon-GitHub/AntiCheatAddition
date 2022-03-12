package de.photon.anticheataddition.util.minecraft.movement;

import lombok.Getter;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public enum Movement
{
    PLAYER(-0.08);

    /**
     * The gravitation that is applied to that type of entity
     */
    @Getter final double gravitationPerTick;

    Movement(double gravitationPerTick)
    {
        this.gravitationPerTick = gravitationPerTick;
    }

    /**
     * This applies the drag of Minecraft's gravitation system (*= 0.98) to a {@link Vector}
     *
     * @param input the input vector (will not be cloned)
     */
    private Vector applyAirResistance(@NotNull Vector input)
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
    @SuppressWarnings({"RedundantCast", "SwitchStatementWithTooFewBranches"})
    public double getJumpYMotion(final Integer amplifier)
    {
        switch (this) {
            case PLAYER:
                double motionY = (double) 0.42F;

                // If the amplifier is null no effect should be applied.
                if (amplifier != null) {
                    // Increase amplifier by one as e.g. amplifier 0 makes up JumpBoost I
                    motionY += (double) ((float) ((amplifier + 1) * 0.1F));
                }
                return motionY;
            default:
                throw new UnsupportedOperationException("Movement type does not support jump y motion calculation.");
        }

    }
}
