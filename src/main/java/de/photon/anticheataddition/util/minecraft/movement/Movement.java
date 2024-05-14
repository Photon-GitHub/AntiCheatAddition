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
     * The gravitation applied to the entity per tick.
     */
    private final double gravitationPerTick;

    Movement(double gravitationPerTick)
    {
        this.gravitationPerTick = gravitationPerTick;
    }

    /**
     * Applies the drag of Minecraft's air resistance system (*= 0.98) to a {@link Vector}.
     *
     * @param input the input vector (will not be cloned)
     *
     * @return the modified vector after applying air resistance
     */
    public static Vector applyAirResistance(@NotNull Vector input)
    {
        return input.multiply((double) 0.98F);
    }

    /**
     * Applies the gravitational force of this entity type to a {@link Vector}.
     *
     * @param input the input vector (will not be cloned)
     *
     * @return the modified vector after applying gravity
     */
    public Vector applyGravitation(@NotNull Vector input)
    {
        return input.setY(input.getY() + this.gravitationPerTick);
    }

    /**
     * Applies both the gravitational force and air resistance of this entity type to a {@link Vector}.
     *
     * @param input the input vector (will not be cloned)
     *
     * @return the modified vector after applying gravity and air resistance
     */
    public Vector applyGravitationAndAirResistance(@NotNull Vector input)
    {
        return applyAirResistance(applyGravitation(input));
    }

    /**
     * Gets the vertical motion (Y-axis) of a {@link org.bukkit.entity.Player} when jumping, taking into account any Jump Boost effects.
     * This method is copied from the client, so any unnecessary casts are kept to ensure the same behavior.
     *
     * @param amplifier the amplifier level of the Jump Boost effect. If no effect should be applied, this should be null.
     *
     * @return the Y motion of the player when jumping
     *
     * @throws UnsupportedOperationException if called on a movement type that does not support jumping
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
}
