package de.photon.AACAdditionPro.util.entities.movement;

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
     * Used for {@link Vector}-Calculations.
     *
     * @return a new {@link Vector} with the acceleration of the entity (0, gravitationalAcceleration, 0)
     */
    public Vector getGravitationalVector()
    {
        return new Vector(0, this.gravitationPerTick, 0);
    }

    /**
     * This applies the drag of Minecraft's gravitation system (*= 0.98) to a {@link Vector}
     */
    public static Vector applyDrag(Vector input)
    {
        return new Vector(input.getX(), input.getY() * 0.98D, input.getZ());
    }
}
