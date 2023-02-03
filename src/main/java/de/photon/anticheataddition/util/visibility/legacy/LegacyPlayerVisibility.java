package de.photon.anticheataddition.util.visibility.legacy;

import de.photon.anticheataddition.util.visibility.PlayerVisibility;
import org.bukkit.entity.Player;

import java.util.Set;

public final class LegacyPlayerVisibility implements PlayerVisibility
{
    private final PlayerInformationHider equipmentHider = new PlayerEquipmentHider();
    private final PlayerInformationHider entityHider = new PlayerHider();

    @Override
    public void setHidden(Player observer, Set<Player> fullyHidden, Set<Player> hideEquipment)
    {
        entityHider.setHiddenEntities(observer, fullyHidden);
        equipmentHider.setHiddenEntities(observer, hideEquipment);
    }
}