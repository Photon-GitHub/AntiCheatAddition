package de.photon.anticheataddition.util.visibility;

import com.comphenix.protocol.ProtocolLibrary;
import com.google.common.collect.Sets;
import de.photon.anticheataddition.AntiCheatAddition;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

class LegacyEntityVisibility implements EntityVisibility
{
    private final EntityInformationHider equipmentHider = new EntityEquipmentHider();
    private final EntityInformationHider playerHider = new EntityHider();

    @Override
    public void setHidden(Player observer, Set<Entity> fullyHidden, Set<Entity> hideEquipment)
    {
        // Run task for the ProtocolLibrary updateEntity.
        val observerList = List.of(observer);
        Bukkit.getScheduler().runTask(AntiCheatAddition.getInstance(), () -> {
            val formerlyFullHidden = playerHider.setHiddenEntities(observer, fullyHidden);
            // Reveal all players here, so the equipment hider can directly use the next.
            for (Entity entity : formerlyFullHidden) ProtocolLibrary.getProtocolManager().updateEntity(entity, observerList);

            val formerlyEquipHidden = equipmentHider.setHiddenEntities(observer, hideEquipment);
            // Now update all entities that are no longer equip hidden and not fully hidden.
            for (Entity entity : Sets.difference(formerlyEquipHidden, fullyHidden)) ProtocolLibrary.getProtocolManager().updateEntity(entity, observerList);
        });
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