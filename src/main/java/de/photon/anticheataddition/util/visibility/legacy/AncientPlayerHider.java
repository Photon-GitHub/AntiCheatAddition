package de.photon.anticheataddition.util.visibility.legacy;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.util.visibility.PacketInformationHider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public final class AncientPlayerHider extends PacketInformationHider
{
    public AncientPlayerHider()
    {
        super(SupportedPacketTypes.ENTITY_EQUIPMENT,
              SupportedPacketTypes.ENTITY_EFFECT,
              SupportedPacketTypes.ENTITY_HEAD_LOOK,
              SupportedPacketTypes.PLAYER_POSITION_AND_LOOK,
              SupportedPacketTypes.ENTITY_METADATA,
              SupportedPacketTypes.ENTITY_STATUS,
              SupportedPacketTypes.ENTITY_TELEPORT,
              SupportedPacketTypes.ENTITY_VELOCITY,
              SupportedPacketTypes.ENTITY_ANIMATION,
              SupportedPacketTypes.SPAWN_LIVING_ENTITY,
              SupportedPacketTypes.COLLECT_ITEM,
              SupportedPacketTypes.ENTITY_RELATIVE_MOVE,
              SupportedPacketTypes.ENTITY_RELATIVE_MOVE_AND_ROTATION,
              SupportedPacketTypes.SPAWN_EXPERIENCE_ORB,
              SupportedPacketTypes.BLOCK_BREAK_ANIMATION,
              SupportedPacketTypes.REMOVE_ENTITY_EFFECT);
    }

    @Override
    protected void onHide(@NotNull Player observer, @NotNull Set<Player> toHide)
    {
        if (toHide.isEmpty()) return;

        final var destroyWrapper = new WrapperPlayServerDestroyEntities(toHide.stream().mapToInt(Entity::getEntityId).toArray());
        PacketEvents.getAPI().getPlayerManager().sendPacket(observer, destroyWrapper);
    }

    @Override
    protected void onReveal(@NotNull Player observer, @NotNull Set<Player> revealed)
    {
        if (revealed.isEmpty()) return;

        final List<Player> observerList = List.of(observer);

        Bukkit.getScheduler().runTask(AntiCheatAddition.getInstance(), () -> {
            for (Entity entity : revealed) ProtocolLibrary.getProtocolManager().updateEntity(entity, observerList);
        });
    }
}