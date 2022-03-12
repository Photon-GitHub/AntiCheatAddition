package de.photon.anticheataddition.util.visibility;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Set;

public interface PlayerVisibility
{
    // In 1.18 there is a new draft api to handle this directly.
    PlayerVisibility INSTANCE = new LegacyPlayerVisibility();

    /**
     * This method will fully hide the toBeHidden {@link Player} from the observer {@link Player}
     */
    void setFullyHidden(Player observer, Set<Entity> toBeHidden);

    /**
     * This method will hide the equipment of the hideEquipment {@link Player} from the observer {@link Player}
     */
    void setEquipmentHidden(Player observer, Set<Entity> hideEquipment);

    void clearPlayer(Player observer);

    void enable();

    void disable();
}
