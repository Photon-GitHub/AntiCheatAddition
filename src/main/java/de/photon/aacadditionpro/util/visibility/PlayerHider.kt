package de.photon.aacadditionpro.util.visibility

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketContainer
import de.photon.aacadditionpro.AACAdditionPro
import org.bukkit.entity.Player
import java.lang.reflect.InvocationTargetException
import java.util.logging.Level

internal class PlayerHider : PlayerInformationHider(PacketType.Play.Server.ENTITY_EQUIPMENT,
                                                    PacketType.Play.Server.ENTITY_EFFECT,
                                                    PacketType.Play.Server.ENTITY_HEAD_ROTATION,
                                                    PacketType.Play.Server.ENTITY_LOOK,
                                                    PacketType.Play.Server.ENTITY_METADATA,
                                                    PacketType.Play.Server.ENTITY_STATUS,
                                                    PacketType.Play.Server.ENTITY_TELEPORT,
                                                    PacketType.Play.Server.ENTITY_VELOCITY,
                                                    PacketType.Play.Server.ANIMATION,
                                                    PacketType.Play.Server.NAMED_ENTITY_SPAWN,
                                                    PacketType.Play.Server.COLLECT,
                                                    PacketType.Play.Server.REL_ENTITY_MOVE,
                                                    PacketType.Play.Server.REL_ENTITY_MOVE_LOOK,
                                                    PacketType.Play.Server.SPAWN_ENTITY_EXPERIENCE_ORB,
                                                    PacketType.Play.Server.BLOCK_BREAK_ANIMATION,
                                                    PacketType.Play.Server.REMOVE_ENTITY_EFFECT) {

    override fun onHide(observer: Player, playerToHide: Player) {
        //Create new packet which destroys the entity
        val destroyEntity = PacketContainer(PacketType.Play.Server.ENTITY_DESTROY)
        destroyEntity.integerArrays.write(0, intArrayOf(playerToHide.entityId))

        // Make the entity disappear
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(observer, destroyEntity)
        } catch (e: InvocationTargetException) {
            AACAdditionPro.getInstance().logger.log(Level.WARNING, "Could not send packet:", e)
        }
    }
}