package de.photon.AACAdditionPro.util.visibility.informationmodifiers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import de.photon.AACAdditionPro.util.visibility.PlayerInformationModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;

public class PlayerHider extends PlayerInformationModifier
{
    @Override
    public void modifyInformation(final Player observer, final Entity entity)
    {
        validate(observer, entity);

        if (setModifyInformation(observer, entity.getEntityId(), false)) {
            //Create new packet which destroys the entity
            final PacketContainer destroyEntity = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
            destroyEntity.getIntegerArrays().write(0, new int[]{entity.getEntityId()});

            // Make the entity disappear
            try {
                manager.sendServerPacket(observer, destroyEntity);
            } catch (final InvocationTargetException e) {
                throw new RuntimeException("Cannot send server packet.", e);
            }
        }
    }

    @Override
    protected PacketType[] getAffectedPackets()
    {
        return new PacketType[]{
                PacketType.Play.Server.ENTITY_EQUIPMENT,
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
                PacketType.Play.Server.REMOVE_ENTITY_EFFECT
        };
    }
}