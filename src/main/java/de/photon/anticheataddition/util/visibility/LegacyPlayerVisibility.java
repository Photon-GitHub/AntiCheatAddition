package de.photon.anticheataddition.util.visibility;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Set;

class LegacyPlayerVisibility implements PlayerVisibility
{
    private final PlayerInformationHider equipmentHider = new PlayerEquipmentHider();
    private final PlayerInformationHider playerHider = new PlayerHider();

    @Override
    public void setFullyHidden(Player observer, Set<Entity> toBeHidden)
    {
        playerHider.setHiddenEntities(observer, toBeHidden);
    }

    @Override
    public void setEquipmentHidden(Player observer, Set<Entity> hideEquipment)
    {
        equipmentHider.setHiddenEntities(observer, hideEquipment);
    }

    @Override
    public void clearPlayer(Player observer)
    {
        playerHider.revealAllEntities(observer);
        equipmentHider.revealAllEntities(observer);
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