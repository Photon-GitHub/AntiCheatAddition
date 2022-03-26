package de.photon.anticheataddition.util.visibility;

import de.photon.anticheataddition.util.visibility.legacy.LegacyEntityVisibility;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Set;

public interface EntityVisibility
{
    // In 1.18 there is a new draft api to handle this directly.
    EntityVisibility INSTANCE = new LegacyEntityVisibility();

    /**
     * Sets fully and equip hidden entities for a {@link Player}.
     */
    void setHidden(Player observer, Set<Entity> fullyHidden, Set<Entity> hideEquipment);

    void enable();

    void disable();
}
