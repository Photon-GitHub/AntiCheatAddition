package de.photon.AACAdditionPro.util.fakeentity.movement;

import lombok.Getter;
import org.bukkit.util.Vector;

public enum Gravitation
{
    PLAYER(-0.08);

    /**
     * The gravitation that is applied to that type of entity
     */
    @Getter
    final double gravitationPerTick;

    Gravitation(double gravitationPerTick)
    {
        this.gravitationPerTick = gravitationPerTick;
    }

    /**
     * This applies the {@link Gravitation} of a specific type to a {@link Vector}
     *
     * @param input       the input vector (will not be cloned)
     * @param gravitation the type of the {@link org.bukkit.entity.Entity} the {@link Gravitation} relates to.
     */
    private static Vector applyGravitation(Vector input, Gravitation gravitation)
    {
        return input.setY(input.getY() + gravitation.gravitationPerTick);
    }

    /**
     * This applies the drag of Minecraft's gravitation system (*= 0.98) to a {@link Vector}
     *
     * @param input the input vector (will not be cloned)
     */
    private static Vector applyAirResistance(Vector input)
    {
        return input.multiply(0.98D);
    }

    /**
     * This applies the {@link Gravitation} and the air resistance of a specific type to a {@link Vector}
     *
     * @param input       the input vector (will not be cloned)
     * @param gravitation the type of the {@link org.bukkit.entity.Entity} the {@link Gravitation} relates to.
     */
    public static Vector applyGravitationAndAirResistance(Vector input, Gravitation gravitation)
    {
        return applyAirResistance(applyGravitation(input, gravitation));
    }
}
