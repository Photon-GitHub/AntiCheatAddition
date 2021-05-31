package de.photon.aacadditionpro.util.visibility;

import com.comphenix.protocol.PacketType;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.util.packetwrappers.sentbyserver.WrapperPlayServerEntityEquipment;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

class PlayerEquipmentHider extends PlayerInformationHider
{
    public PlayerEquipmentHider()
    {
        super(PacketType.Play.Server.ENTITY_EQUIPMENT);
    }

    @Override
    protected void onHide(@NotNull Player observer, @NotNull Player playerToHide)
    {
        Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> WrapperPlayServerEntityEquipment.clearAllSlots(playerToHide.getEntityId(), observer));
    }

    @Override
    protected Set<ServerVersion> getSupportedVersions()
    {
        return ServerVersion.NON_188_VERSIONS;
    }
}