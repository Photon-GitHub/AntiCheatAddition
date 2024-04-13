package de.photon.anticheataddition.util.visibility;

import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.util.visibility.legacy.LegacyPlayerEquipmentHider;
import de.photon.anticheataddition.util.visibility.legacy.LegacyPlayerHider;
import de.photon.anticheataddition.util.visibility.modern.ModernPlayerEquipmentHider;
import de.photon.anticheataddition.util.visibility.modern.ModernPlayerHider;
import org.bukkit.entity.Player;

import java.util.Set;

public final class PlayerVisibilityImpl implements PlayerVisibility
{
    private final PlayerInformationHider equipmentHider = ServerVersion.MC119.activeIsLaterOrEqual() ? new ModernPlayerEquipmentHider() : new LegacyPlayerEquipmentHider();
    private final PlayerInformationHider entityHider = ServerVersion.MC120.activeIsLaterOrEqual() ? new ModernPlayerHider() : new LegacyPlayerHider();

    @Override
    public void setHidden(Player observer, Set<Player> fullyHidden, Set<Player> hideEquipment)
    {
        entityHider.setHiddenEntities(observer, fullyHidden);
        equipmentHider.setHiddenEntities(observer, hideEquipment);
    }
}