package de.photon.anticheataddition.modules.checks.targeting;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAttachEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.subdata.TargetingRotationStepData;
import de.photon.anticheataddition.util.minecraft.world.entity.EntityUtil;
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Action-associated yaw-step analysis with conservative sensitivity relearning.
 */
public final class TargetingRotationStep extends ViolationModule implements Listener
{
    public static final TargetingRotationStep INSTANCE = new TargetingRotationStep();

    private TargetingRotationStep()
    {
        super("Targeting.parts.RotationStep");
    }

    private void receive(final PacketReceiveEvent event)
    {
        final User user = User.getUser(event);
        if (user == null) return;
        final var state = user.getData().object.targetingRotationStepData;
        final long now = System.nanoTime();
        final ClientVersion client = event.getUser().getClientVersion();
        if (User.isUserInvalid(user, this) || client == null ||
            client == ClientVersion.UNKNOWN || client.isOlderThan(ClientVersion.V_1_8)) {
            state.reset();
            return;
        }
        if (event.isCancelled()) {
            // A rejected action cannot erase rotation evidence. Missing movement does break continuity.
            if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) state.reset();
            return;
        }
        if (user.getData().object.packetFloodData.isThrottled(now)) {
            state.suppress(now);
            return;
        }
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            final var packet = new WrapperPlayClientPlayerFlying(event);
            final var report = state.movement(packet.getLocation().getYaw(), packet.hasRotationChanged(), now);
            if (report != null) report(user, report);
            return;
        }
        switch (event.getPacketType()) {
            case PacketType.Play.Client.ATTACK, PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT, PacketType.Play.Client.USE_ITEM -> state.action();
            case PacketType.Play.Client.INTERACT_ENTITY -> {
                final var packet = new WrapperPlayClientInteractEntity(event);
                if (packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.INTERACT &&
                    client.isOlderThanOrEquals(ClientVersion.V_1_13) && state.isLegacyHorse(packet.getEntityId())) {
                    // Only this legacy interaction can force yaw without actually mounting.
                    state.suppress(now);
                } else {
                    state.action();
                }
            }
            default -> { /* Client claims about vehicles, beds or flight do not grant exemptions. */ }

        }
    }

    private void report(final User user, final TargetingRotationStepData.Report report)
    {
        getManagement().flag(Flag.of(user)
                                 .setAddedVl(15)
                                 .setDebug(() -> "TargetingData-Debug | Player: " + user.getPlayer().getName() + " repeatedly departed from and returned to their ordinary yaw step around actions (step: " + report.step() + ", episodes: " + report.episodes() + ")."));
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .addPacketListeners(PacketAdapterBuilder.of(this,
                                                                       PacketType.Play.Client.PLAYER_FLYING,
                                                                       PacketType.Play.Client.PLAYER_POSITION,
                                                                       PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION,
                                                                       PacketType.Play.Client.PLAYER_ROTATION,
                                                                       PacketType.Play.Client.INTERACT_ENTITY,
                                                                       PacketType.Play.Client.ATTACK,
                                                                       PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT,
                                                                       PacketType.Play.Client.USE_ITEM)
                                                                   .priority(PacketListenerPriority.MONITOR)
                                                                   .onReceivingRaw(this::receive).build())
                           .addPacketListeners(PacketAdapterBuilder.of(this,
                                                                       PacketType.Play.Server.PLAYER_POSITION_AND_LOOK,
                                                                       PacketType.Play.Server.RESPAWN,
                                                                       PacketType.Play.Server.CAMERA,
                                                                       PacketType.Play.Server.SET_PASSENGERS,
                                                                       PacketType.Play.Server.ATTACH_ENTITY,
                                                                       PacketType.Play.Server.CHANGE_GAME_STATE,
                                                                       PacketType.Play.Server.SPAWN_ENTITY,
                                                                       PacketType.Play.Server.SPAWN_LIVING_ENTITY,
                                                                       PacketType.Play.Server.DESTROY_ENTITIES)
                                                                   .priority(PacketListenerPriority.MONITOR)
                                                                   .onSendingRaw(this::send).build())
                           .build();
    }

    private void send(final PacketSendEvent event)
    {
        if (event.isCancelled()) return;
        final User user = User.getUser(event);
        if (user == null) return;
        final var state = user.getData().object.targetingRotationStepData;
        final long now = System.nanoTime();
        switch (event.getPacketType()) {
            case PacketType.Play.Server.SPAWN_ENTITY -> {
                final var packet = new WrapperPlayServerSpawnEntity(event);
                state.trackLegacyHorse(packet.getEntityId(), EntityTypes.isTypeInstanceOf(packet.getEntityType(), EntityTypes.ABSTRACT_HORSE));
            }
            case PacketType.Play.Server.SPAWN_LIVING_ENTITY -> {
                final var packet = new WrapperPlayServerSpawnLivingEntity(event);
                state.trackLegacyHorse(packet.getEntityId(), EntityTypes.isTypeInstanceOf(packet.getEntityType(), EntityTypes.ABSTRACT_HORSE));
            }
            case PacketType.Play.Server.DESTROY_ENTITIES -> {
                state.removeEntities(new WrapperPlayServerDestroyEntities(event).getEntityIds());
            }
            case PacketType.Play.Server.SET_PASSENGERS -> {
                final var packet = new WrapperPlayServerSetPassengers(event);
                if (state.passengers(packet.getEntityId(), packet.getPassengers(), event.getUser().getEntityId())) state.suppress(now);
            }
            case PacketType.Play.Server.ATTACH_ENTITY -> {
                final var packet = new WrapperPlayServerAttachEntity(event);
                if (!packet.isLeash() && packet.getAttachedId() == event.getUser().getEntityId()) state.suppress(now);
            }
            case PacketType.Play.Server.CHANGE_GAME_STATE -> {
                if (new WrapperPlayServerChangeGameState(event).getReason() == WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE) state.suppress(now);
            }
            case PacketType.Play.Server.RESPAWN -> {
                state.clearEntities();
                state.suppress(now);
            }
            default -> state.suppress(now);
        }
    }

    /**
     * Bukkit state is inspected only on the server thread, never from the packet listener.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(final PlayerMoveEvent event)
    {
        final var player = event.getPlayer();
        if (event.isCancelled() || player.isInsideVehicle() || player.isFlying() || player.getGameMode() == GameMode.SPECTATOR || EntityUtil.INSTANCE.isFlyingWithElytra(player)) {
            final User user = User.getUser(player);
            if (user != null) user.getData().object.targetingRotationStepData.suppress(System.nanoTime());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(final PlayerInteractEvent event)
    {
        if (event.useItemInHand() != org.bukkit.event.Event.Result.DENY &&
            (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) &&
            event.getItem() != null && event.getItem().getType().name().equals("SPYGLASS")) {
            final User user = User.getUser(event.getPlayer());
            if (user != null) user.getData().object.targetingRotationStepData.suppress(System.nanoTime());
        }
    }

    @Override
    protected void disable()
    {
        for (final User user : User.getUsersUnwrapped()) user.getData().object.targetingRotationStepData.reset();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).emptyThresholdManagement().withDecay(400, 2).build();
    }
}
