package de.photon.AACAdditionPro.util.fakeentity.movement;

import de.photon.AACAdditionPro.api.killauraentity.MovementType;
import org.bukkit.Location;

public interface Movement
{
    /**
     * Calculate the next position where the entity should be
     *
     * @param playerLocation the current position of the observed player.
     * @param old            the last location of the entity.
     *
     * @return the new position of the entity or null when this state has no more movements to offer
     */
    Location calculate(Location playerLocation, Location old);

    MovementType getMovementType();

    boolean shouldSprint();

    boolean jumpIfCollidedHorizontally();

    boolean isTPNeeded();
}
