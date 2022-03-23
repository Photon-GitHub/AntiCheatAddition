package de.photon.anticheataddition.util.visibility;

import com.comphenix.protocol.PacketType;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.protocol.packetwrappers.sentbyserver.equipment.IWrapperPlayEquipment;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

final class PlayerEquipmentHider extends PlayerInformationHider
{
    public PlayerEquipmentHider()
    {
        super(PacketType.Play.Server.ENTITY_EQUIPMENT);
    }

    @Override
    protected void onHide(@NotNull Player observer, @NotNull Set<Entity> toHide)
    {
        Bukkit.getScheduler().runTask(AntiCheatAddition.getInstance(), () -> {
            for (Entity entity : toHide) IWrapperPlayEquipment.clearAllSlots(entity.getEntityId(), observer);
        });
    }

    @Override
    protected Set<ServerVersion> getSupportedVersions()
    {
        return ServerVersion.NON_188_VERSIONS;
    }
}