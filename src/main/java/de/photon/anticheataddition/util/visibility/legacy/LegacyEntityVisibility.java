package de.photon.anticheataddition.util.visibility.legacy;

import com.google.common.collect.Sets;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.util.visibility.EntityVisibility;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Set;

public class LegacyEntityVisibility implements EntityVisibility
{
    private final EntityInformationHider equipmentHider;
    private final EntityInformationHider entityHider;

    public LegacyEntityVisibility()
    {
        this.equipmentHider = new EntityEquipmentHider();
        this.entityHider = new EntityHider();

        this.equipmentHider.registerListeners();
        this.entityHider.registerListeners();
    }

    @Override
    public void setHidden(Player observer, Set<Entity> fullyHidden, Set<Entity> hideEquipment)
    {
        // Run task for the ProtocolLibrary updateEntity.
        Bukkit.getScheduler().runTask(AntiCheatAddition.getInstance(), () -> {
            val formerlyFullHidden = entityHider.setHiddenEntities(observer, fullyHidden);
            // Reveal all players here, so the equipment hider can directly use the next.
            entityHider.onReveal(observer, formerlyFullHidden);

            val formerlyEquipHidden = equipmentHider.setHiddenEntities(observer, hideEquipment);
            // Now update all entities that are no longer equip hidden and not fully hidden.
            equipmentHider.onReveal(observer, Sets.difference(formerlyEquipHidden, fullyHidden));
        });
    }
}