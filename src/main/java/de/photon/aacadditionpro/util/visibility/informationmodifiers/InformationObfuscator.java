package de.photon.aacadditionpro.util.visibility.informationmodifiers;

import com.comphenix.protocol.PacketType;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.util.packetwrappers.server.WrapperPlayServerEntityEquipment;
import de.photon.aacadditionpro.util.visibility.PlayerInformationModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Set;

public class InformationObfuscator extends PlayerInformationModifier
{
    @Override
    public void modifyInformation(final Player observer, final Entity entity)
    {
        validate(observer, entity);

        if (setModifyInformation(observer, entity.getEntityId(), false)) {
            WrapperPlayServerEntityEquipment.clearAllSlots(entity.getEntityId(), observer);
        }
    }

    @Override
    protected PacketType[] getAffectedPackets()
    {
        return new PacketType[]{PacketType.Play.Server.ENTITY_EQUIPMENT};
    }

    @Override
    protected Set<ServerVersion> getSupportedVersions()
    {
        return ServerVersion.NON_188_VERSIONS;
    }
}