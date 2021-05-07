package de.photon.aacadditionpro.util.visibility

import com.comphenix.protocol.PacketType
import de.photon.aacadditionpro.AACAdditionPro
import de.photon.aacadditionpro.ServerVersion
import de.photon.aacadditionpro.util.packetwrappers.server.WrapperPlayServerEntityEquipment
import org.bukkit.Bukkit
import org.bukkit.entity.Player

internal class PlayerEquipmentHider : PlayerInformationHider(PacketType.Play.Server.ENTITY_EQUIPMENT) {
    override fun onHide(observer: Player, playerToHide: Player) {
        Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), Runnable { WrapperPlayServerEntityEquipment.clearAllSlots(playerToHide.entityId, observer) })
    }

    override val supportedVersions: Set<ServerVersion> get() = ServerVersion.NON_188_VERSIONS
}