package de.photon.anticheataddition.util.visibility.legacy;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.protocol.packetwrappers.sentbyserver.entitydestroy.IWrapperServerEntityDestroy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class EntityHider extends EntityInformationHider
{
    public EntityHider()
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

    @Override
    protected void onReveal(@NotNull Player observer, @NotNull Set<Entity> revealed)
    {
        final List<Player> observerList = List.of(observer);
        Bukkit.getScheduler().runTask(AntiCheatAddition.getInstance(), () -> {
            for (Entity entity : revealed) {
                ProtocolLibrary.getProtocolManager().updateEntity(entity, observerList);
            }
        });
        for (Entity entity : revealed) ProtocolLibrary.getProtocolManager().updateEntity(entity, observerList);
    }
}