package de.photon.anticheataddition.util.visibility;

import org.bukkit.entity.Player;

import java.util.Set;

public sealed interface PlayerVisibility permits PlayerVisibilityImpl
{
    PlayerVisibility INSTANCE = new PlayerVisibilityImpl();

    /**
     * Sets fully and equip hidden {@link Player}s for a {@link Player}.
     * Can be called asynchronously.
     */
    void setHidden(Player observer, Set<Player> fullyHidden, Set<Player> hideEquipment);
}
