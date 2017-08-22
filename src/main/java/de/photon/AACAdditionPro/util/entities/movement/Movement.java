package de.photon.AACAdditionPro.util.entities.movement;

import de.photon.AACAdditionPro.api.killauraentity.MovementType;
import org.bukkit.Location;

public interface Movement
{
    /**
     * Calculate the next position where the entity should be
     *
     * @return the new position of the entity or null when this state has no more movements to offer
     */
    Location calculate(Location old);

    MovementType getMovementType();

    boolean jumpIfCollidedHorizontally();
}
