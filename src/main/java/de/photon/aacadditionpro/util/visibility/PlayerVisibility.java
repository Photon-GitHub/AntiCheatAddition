package de.photon.aacadditionpro.util.visibility;

import org.bukkit.entity.Player;

public interface PlayerVisibility
{
    // In 1.18 there is a new draft api to handle this directly.
    PlayerVisibility INSTANCE = new LegacyPlayerVisibility();

    /**
     * This method will fully hide the toBeHidden {@link Player} from the observer {@link Player}
     */
    void fullyHidePlayer(Player observer, Player toBeHidden);

    /**
     * This method will hide the equipment of the hideEquipment {@link Player} from the observer {@link Player}
     */
    void hideEquipment(Player observer, Player hideEquipment);

    /**
     * This method will fully reveal the toBeRevealed {@link Player} from the observer {@link Player}
     */
    void revealPlayer(Player observer, Player toBeRevealed);

    void enable();

    void disable();
}
