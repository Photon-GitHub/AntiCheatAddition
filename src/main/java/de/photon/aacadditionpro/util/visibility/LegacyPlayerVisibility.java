package de.photon.aacadditionpro.util.visibility;

import org.bukkit.entity.Player;

class LegacyPlayerVisibility implements PlayerVisibility
{
    private final PlayerInformationHider equipmentHider = new PlayerEquipmentHider();
    private final PlayerInformationHider playerHider = new PlayerHider();

    public void fullyHidePlayer(Player observer, Player toBeHidden)
    {
        playerHider.hidePlayer(observer, toBeHidden);
        equipmentHider.revealPlayer(observer, toBeHidden);
    }

    public void hideEquipment(Player observer, Player hideEquipment)
    {
        equipmentHider.hidePlayer(observer, hideEquipment);
        playerHider.revealPlayer(observer, hideEquipment);
    }

    public void revealPlayer(Player observer, Player toBeRevealed)
    {
        playerHider.revealPlayer(observer, toBeRevealed);
        equipmentHider.revealPlayer(observer, toBeRevealed);
    }

    public void enable()
    {
        equipmentHider.registerListeners();
        playerHider.registerListeners();
    }

    public void disable()
    {
        equipmentHider.unregisterListeners();
        equipmentHider.clear();
        playerHider.unregisterListeners();
        playerHider.clear();
    }
}