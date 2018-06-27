package de.photon.AACAdditionPro.api.killauraentity;

import de.photon.AACAdditionPro.util.fakeentity.movement.submovements.BasicFollowMovement;
import de.photon.AACAdditionPro.util.fakeentity.movement.submovements.StayMovement;
import org.bukkit.Location;

public interface Movement
{
    Movement STAY_MOVEMENT = new StayMovement();
    Movement BASIC_FOLLOW_MOVEMENT = new BasicFollowMovement();

    /**
     * Calculate the next position where the entity should be
     *
     * @param playerLocation    the current position of the observed player.
     * @param oldEntityLocation the last location of the entity.
     *
     * @return the new position of the entity or null when this state has no more movements to offer
     */
    Location calculate(Location playerLocation, Location oldEntityLocation);

    /**
     * Whether or not the entity should sprint in the next tick
     */
    boolean shouldSprint();

    boolean jumpIfCollidedHorizontally();

    /**
     * Whether or not the entity should tp in the next tick
     */
    boolean isTPNeeded();
}
