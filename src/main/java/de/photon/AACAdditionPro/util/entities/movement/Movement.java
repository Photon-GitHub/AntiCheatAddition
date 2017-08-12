package de.photon.AACAdditionPro.util.entities.movement;

import org.bukkit.Location;

/**
 * @author geNAZt
 * @version 1.0
 */
public abstract class Movement
{
    /**
     * Calculate the next position where the entity should be
     *
     * @return the new position of the entity or null when this state has no more movements to offer
     */
    public abstract Location calculate();
}
