package de.photon.anticheataddition.util.visibility.legacy;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.util.visibility.EntityVisibility;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Set;

public final class LegacyEntityVisibility implements EntityVisibility
{
    private final EntityInformationHider equipmentHider = new EntityEquipmentHider();
    private final EntityInformationHider entityHider = new EntityHider();

    @Override
    public void setHidden(Player observer, Set<Entity> fullyHidden, Set<Entity> hideEquipment)
    {
        // Run task for the ProtocolLibrary updateEntity.
        Bukkit.getScheduler().runTask(AntiCheatAddition.getInstance(), () -> {
            entityHider.setHiddenEntities(observer, fullyHidden);
            equipmentHider.setHiddenEntities(observer, hideEquipment);
        });
    }
}