package de.photon.aacadditionpro.util.visibility;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.entity.Player;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayerVisibility
{
    private static final PlayerVisibility instance = new PlayerVisibility();

    private final PlayerInformationHider equipmentHider = new PlayerEquipmentHider();
    private final PlayerInformationHider playerHider = new PlayerHider();

    /**
     * This method will fully hide the toBeHidden {@link Player} from the observer {@link Player}
     */
    public void fullyHidePlayer(Player observer, Player toBeHidden)
    {
        playerHider.hidePlayer(observer, toBeHidden);
        equipmentHider.revealPlayer(observer, toBeHidden);
    }

    /**
     * This method will hide the equipment of the hideEquipment {@link Player} from the observer {@link Player}
     */
    public void hideEquipment(Player observer, Player hideEquipment)
    {
        equipmentHider.hidePlayer(observer, hideEquipment);
        playerHider.revealPlayer(observer, hideEquipment);
    }

    /**
     * This method will fully reveal the toBeRevealed {@link Player} from the observer {@link Player}
     */
    public void revealPlayer(Player observer, Player toBeRevealed)
    {
        playerHider.revealPlayer(observer, toBeRevealed);
        equipmentHider.revealPlayer(observer, toBeRevealed);
    }

    public void enable()
    {
        this.equipmentHider.registerListeners();
        this.playerHider.registerListeners();
    }

    public void disable()
    {
        this.equipmentHider.unregisterListeners();
        this.equipmentHider.clear();
        this.playerHider.unregisterListeners();
        this.playerHider.clear();
    }
}
