package de.photon.AACAdditionPro.util.visibility.informationmodifiers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.wrappers.EnumWrappers;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.util.multiversion.ServerVersion;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityEquipment;
import de.photon.AACAdditionPro.util.visibility.PlayerInformationModifier;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class InformationObfuscator extends PlayerInformationModifier
{
    @Override
    public void modifyInformation(final Player observer, final Entity entity)
    {
        switch (AACAdditionPro.getInstance().getServerVersion()) {
            case MC188:
                // Protocol-incompatibility with 1.8
                return;
            case MC110:
            case MC111:
            case MC112:
                validate(observer, entity);
                setModifyInformation(observer, entity.getEntityId(), false);

                for (final EnumWrappers.ItemSlot slot : EnumWrappers.ItemSlot.values()) {
                    //Update the equipment with fake-packets
                    final WrapperPlayServerEntityEquipment wrapperPlayServerEntityEquipment = new WrapperPlayServerEntityEquipment();

                    wrapperPlayServerEntityEquipment.setEntityID(entity.getEntityId());
                    wrapperPlayServerEntityEquipment.setItem(new ItemStack(Material.AIR));


                    // 1.8.8 is automatically included as of the bukkit-handling, therefore server-version specific handling
                    // as of the different server classes / enums and the null-removal above.
                    wrapperPlayServerEntityEquipment.setSlot(slot);
                    wrapperPlayServerEntityEquipment.sendPacket(observer);
                }
                break;
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
        return new HashSet<>(Arrays.asList(ServerVersion.MC110, ServerVersion.MC111, ServerVersion.MC112));
    }
}