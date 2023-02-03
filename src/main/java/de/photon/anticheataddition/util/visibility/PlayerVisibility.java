package de.photon.anticheataddition.util.visibility;

import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.util.visibility.legacy.LegacyPlayerVisibility;
import de.photon.anticheataddition.util.visibility.modern.ModernPlayerVisibility;
import org.bukkit.entity.Player;

import java.util.Set;

public interface PlayerVisibility
{
    PlayerVisibility INSTANCE = ServerVersion.containsActive(ServerVersion.MC119.getSupVersionsFrom()) ? new ModernPlayerVisibility() : new LegacyPlayerVisibility();

    /**
     * Sets fully and equip hidden {@link Player}s for a {@link Player}.
     */
    void setHidden(Player observer, Set<Player> fullyHidden, Set<Player> hideEquipment);
}
