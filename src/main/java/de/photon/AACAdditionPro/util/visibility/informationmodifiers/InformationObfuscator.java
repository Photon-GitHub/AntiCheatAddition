package de.photon.AACAdditionPro.util.visibility.informationmodifiers;

import com.comphenix.protocol.PacketType;
import de.photon.AACAdditionPro.Module;
import de.photon.AACAdditionPro.ServerVersion;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityEquipment;
import de.photon.AACAdditionPro.util.visibility.PlayerInformationModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Set;

public class InformationObfuscator extends PlayerInformationModifier
{
    @Override
    public void modifyInformation(final Player observer, final Entity entity)
    {
        validate(observer, entity);
        setModifyInformation(observer, entity.getEntityId(), false);

        WrapperPlayServerEntityEquipment.clearAllSlots(entity.getEntityId(), observer);
    }

    @Override
    protected PacketType[] getAffectedPackets()
    {
        return new PacketType[]{PacketType.Play.Server.ENTITY_EQUIPMENT};
    }

    @Override
    protected Set<ServerVersion> getSupportedVersions()
    {
        return Module.NON_188_VERSIONS;
    }
}