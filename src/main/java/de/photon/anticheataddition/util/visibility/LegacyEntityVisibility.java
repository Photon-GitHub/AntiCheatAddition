package de.photon.anticheataddition.util.visibility;

import com.comphenix.protocol.ProtocolLibrary;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.util.messaging.DebugSender;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class LegacyEntityVisibility implements EntityVisibility
{
    private final EntityInformationHider equipmentHider = new EntityEquipmentHider();
    private final EntityInformationHider playerHider = new EntityHider();

    public static void updateEntities(@NotNull Player observer, Collection<Entity> entities)
    {
        // Performance optimization for no changes.
        if (entities.isEmpty() || ProtocolLibrary.getProtocolManager() == null) return;

        DebugSender.getInstance().sendDebug("Update Entities for " + observer.getName() + " entities: " + entities.stream().map(Entity::getName).collect(Collectors.joining(", ")), true, false);
        final List<Player> playerList = List.of(observer);
        Bukkit.getScheduler().runTask(AntiCheatAddition.getInstance(), () -> {
            for (Entity entity : entities) ProtocolLibrary.getProtocolManager().updateEntity(entity, playerList);
        });
    }

    @Override
    public void setHidden(Player observer, Set<Entity> fullyHidden, Set<Entity> hideEquipment)
    {
        final Set<Entity> entitiesToUpdate = new HashSet<>();

        entitiesToUpdate.addAll(playerHider.setHiddenEntities(observer, fullyHidden));
        entitiesToUpdate.addAll(equipmentHider.setHiddenEntities(observer, hideEquipment));

        entitiesToUpdate.removeAll(fullyHidden);
        entitiesToUpdate.removeAll(hideEquipment);

        updateEntities(observer, entitiesToUpdate);
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