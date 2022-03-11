package de.photon.aacadditionpro.util.visibility;

import com.comphenix.protocol.PacketType;
import de.photon.aacadditionpro.protocol.packetwrappers.sentbyserver.entitydestroy.IWrapperServerEntityDestroy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.stream.Collectors;

class PlayerHider extends PlayerInformationHider
{
    public PlayerHider()
    {
        super(PacketType.Play.Server.ENTITY_EQUIPMENT,
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
              PacketType.Play.Server.REMOVE_ENTITY_EFFECT);
    }

    @Override
    protected void onHide(@NotNull Player observer, @NotNull Set<Entity> toHide)
    {
        IWrapperServerEntityDestroy.sendDestroyEntities(observer, toHide.stream().map(Entity::getEntityId).collect(Collectors.toList()));
    }
}