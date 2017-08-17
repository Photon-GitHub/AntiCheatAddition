package de.photon.AACAdditionPro.util.entities.movement;

import org.bukkit.Location;

public abstract class Movement
{
    /**
     * Calculate the next position where the entity should be
     *
     * @return the new position of the entity or null when this state has no more movements to offer
     */
    public abstract Location calculate(Location old);
}
