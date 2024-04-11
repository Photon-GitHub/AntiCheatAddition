package de.photon.anticheataddition.util.visibility;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import de.photon.anticheataddition.ServerVersion;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class PacketInformationHider extends PlayerInformationHider implements Listener
{
    protected PacketInformationHider(@NotNull SupportedPacketTypes... affectedPackets)
    {
        super();

        // Only start if the ServerVersion is supported
        if (!ServerVersion.containsActive(this.getSupportedVersions()) || affectedPackets.length == 0) return;

        final Set<PacketTypeCommon> packetTypeSet = Arrays.stream(affectedPackets).map(SupportedPacketTypes::getPacketEventsPacketType).collect(Collectors.toSet());

        PacketEvents.getAPI().getEventManager().registerListener(
                // Get all hidden entities
                // The test for the entityId must happen here in the synchronized block as get only returns a view that might change async.
                new PacketListenerAbstract()
                {
                    @Override
                    public void onPacketSend(PacketSendEvent event)
                    {
                        if (!packetTypeSet.contains(event.getPacketType())
                            || event.isCancelled()) return;

                        final int entityId = SupportedPacketTypes.getEntityId(event);

                        final boolean cancelPacket;
                        synchronized (hiddenFromPlayerMap) {
                            // The test for the entityId must happen here in the synchronized block as get only returns a view that might change async.
                            cancelPacket = hiddenFromPlayerMap.get((Player) event.getPlayer()).stream()
                                                              .mapToInt(Player::getEntityId)
                                                              .anyMatch(i -> i == entityId);
                        }

                        if (cancelPacket) event.setCancelled(true);
                    }
                });
    }

    @Getter
    protected enum SupportedPacketTypes
    {
        BLOCK_BREAK_ANIMATION(PacketType.Play.Server.BLOCK_BREAK_ANIMATION),
        COLLECT_ITEM(PacketType.Play.Server.COLLECT_ITEM),
        ENTITY_ANIMATION(PacketType.Play.Server.ENTITY_ANIMATION),
        ENTITY_EFFECT(PacketType.Play.Server.ENTITY_EFFECT),
        ENTITY_EQUIPMENT(PacketType.Play.Server.ENTITY_EQUIPMENT),
        ENTITY_HEAD_LOOK(PacketType.Play.Server.ENTITY_HEAD_LOOK),
        ENTITY_METADATA(PacketType.Play.Server.ENTITY_METADATA),
        ENTITY_RELATIVE_MOVE(PacketType.Play.Server.ENTITY_RELATIVE_MOVE),
        ENTITY_RELATIVE_MOVE_AND_ROTATION(PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION),
        ENTITY_STATUS(PacketType.Play.Server.ENTITY_STATUS),
        ENTITY_TELEPORT(PacketType.Play.Server.ENTITY_TELEPORT),
        ENTITY_VELOCITY(PacketType.Play.Server.ENTITY_VELOCITY),
        HURT_ANIMATION(PacketType.Play.Server.HURT_ANIMATION),
        PLAYER_POSITION_AND_LOOK(PacketType.Play.Server.PLAYER_POSITION_AND_LOOK),
        REMOVE_ENTITY_EFFECT(PacketType.Play.Server.REMOVE_ENTITY_EFFECT),
        SPAWN_EXPERIENCE_ORB(PacketType.Play.Server.SPAWN_EXPERIENCE_ORB),
        SPAWN_LIVING_ENTITY(PacketType.Play.Server.SPAWN_LIVING_ENTITY);

        private final PacketTypeCommon packetEventsPacketType;

        private static final Map<PacketTypeCommon, SupportedPacketTypes> BACKWARDS_MAP = Arrays.stream(values())
                                                                                               .collect(Collectors.toMap(SupportedPacketTypes::getPacketEventsPacketType, e -> e));

        SupportedPacketTypes(PacketTypeCommon packetEventsPacketType)
        {
            this.packetEventsPacketType = packetEventsPacketType;
        }

        public static int getEntityId(PacketSendEvent event)
        {
            final var packetType = event.getPacketType();
            final var supportedPacketType = BACKWARDS_MAP.get(packetType);

            if (supportedPacketType == null) throw new IllegalArgumentException("Unsupported packet type: " + packetType);

            return switch (supportedPacketType) {
                case BLOCK_BREAK_ANIMATION -> new WrapperPlayServerBlockBreakAnimation(event).getEntityId();
                case COLLECT_ITEM -> new WrapperPlayServerCollectItem(event).getCollectorEntityId();
                case ENTITY_ANIMATION -> new WrapperPlayServerEntityAnimation(event).getEntityId();
                case ENTITY_EFFECT -> new WrapperPlayServerEntityEffect(event).getEntityId();
                case ENTITY_EQUIPMENT -> new WrapperPlayServerEntityEquipment(event).getEntityId();
                case ENTITY_HEAD_LOOK -> new WrapperPlayServerEntityHeadLook(event).getEntityId();
                case ENTITY_METADATA -> new WrapperPlayServerEntityMetadata(event).getEntityId();
                case ENTITY_RELATIVE_MOVE -> new WrapperPlayServerEntityRelativeMove(event).getEntityId();
                case ENTITY_RELATIVE_MOVE_AND_ROTATION -> new WrapperPlayServerEntityRelativeMoveAndRotation(event).getEntityId();
                case ENTITY_STATUS -> new WrapperPlayServerEntityStatus(event).getEntityId();
                case ENTITY_TELEPORT -> new WrapperPlayServerEntityTeleport(event).getEntityId();
                case ENTITY_VELOCITY -> new WrapperPlayServerEntityVelocity(event).getEntityId();
                case HURT_ANIMATION -> new WrapperPlayServerHurtAnimation(event).getEntityId();
                case PLAYER_POSITION_AND_LOOK -> ((Player) event.getPlayer()).getEntityId();
                case REMOVE_ENTITY_EFFECT -> new WrapperPlayServerRemoveEntityEffect(event).getEntityId();
                case SPAWN_EXPERIENCE_ORB -> new WrapperPlayServerSpawnExperienceOrb(event).getEntityId();
                case SPAWN_LIVING_ENTITY -> new WrapperPlayServerSpawnLivingEntity(event).getEntityId();
            };
        }
    }
}
