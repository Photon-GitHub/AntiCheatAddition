package de.photon.anticheataddition.util.visibility;

import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.util.visibility.legacy.AncientPlayerHider;
import de.photon.anticheataddition.util.visibility.legacy.LegacyPlayerEquipmentHider;
import de.photon.anticheataddition.util.visibility.modern.ModernPlayerEquipmentHider;
import org.bukkit.entity.Player;

import java.util.Set;

public final class PlayerVisibilityImpl implements PlayerVisibility
{
    private final PlayerInformationHider equipmentHider = ServerVersion.containsActive(ServerVersion.MC119.getSupVersionsFrom()) ? new ModernPlayerEquipmentHider() : new LegacyPlayerEquipmentHider();
    private final PlayerInformationHider entityHider = new AncientPlayerHider();

    @Override
    public void setHidden(Player observer, Set<Player> fullyHidden, Set<Player> hideEquipment)
    {
        entityHider.setHiddenEntities(observer, fullyHidden);
        equipmentHider.setHiddenEntities(observer, hideEquipment);
    }
}