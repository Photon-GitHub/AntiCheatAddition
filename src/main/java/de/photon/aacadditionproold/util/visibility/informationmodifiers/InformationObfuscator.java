package de.photon.aacadditionproold.util.visibility.informationmodifiers;

import com.comphenix.protocol.PacketType;
import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionproold.AACAdditionPro;
import de.photon.aacadditionproold.ServerVersion;
import de.photon.aacadditionproold.util.packetwrappers.server.WrapperPlayServerEntityEquipment;
import de.photon.aacadditionproold.util.visibility.PlayerInformationModifier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Set;

public class InformationObfuscator extends PlayerInformationModifier
{
    protected static final Set<PacketType> AFFECTED_PACKET_TYPES = ImmutableSet.of(PacketType.Play.Server.ENTITY_EQUIPMENT);

    @Override
    public void modifyInformation(final Player observer, final Entity entity)
    {
        validate(observer, entity);

        if (setModifyInformation(observer, entity.getEntityId(), false)) {
            Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> WrapperPlayServerEntityEquipment.clearAllSlots(entity.getEntityId(), observer));
        }
    }

    @Override
    protected Set<PacketType> getAffectedPackets()
    {
        return AFFECTED_PACKET_TYPES;
    }

    @Override
    protected Set<ServerVersion> getSupportedVersions()
    {
        return ServerVersion.NON_188_VERSIONS;
    }
}