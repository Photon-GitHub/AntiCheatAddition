package de.photon.anticheataddition.modules.checks.targeting;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.subdata.TargetingData;
import de.photon.anticheataddition.user.data.subdata.TargetingReplayData;
import de.photon.anticheataddition.util.minecraft.world.WorldUtil;
import de.photon.anticheataddition.util.minecraft.world.entity.EntityUtil;
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.util.violationlevels.ViolationAggregation;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import de.photon.anticheataddition.util.violationlevels.threshold.ThresholdManagement;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.stream.Collectors;

/**
 * Parent module for interaction-aware targeting checks.
 *
 * <p>The parent owns the packet listener and shared history so every movement packet is collected once and every
 * interaction window is analyzed once. Rotation-less movement packets repeat the most recently known look direction;
 * otherwise a client could bypass zero-noise analysis simply by omitting yaw and pitch while they remain unchanged.</p>
 *
 * <p>Packet gaps, flight, vehicles, and abrupt rotations are not blanket exemptions. Discontinuities are evaluated by
 * their own submodule and are also made robust for the statistical checks. A server-confirmed teleport marks a trusted
 * boundary on the next client movement packet without clearing earlier samples, accumulated evidence, or replay
 * fingerprints.</p>
 */
public final class Targeting extends ViolationModule implements Listener
{
    public static final Targeting INSTANCE = new Targeting();

    private static final double MAXIMUM_TARGET_HORIZONTAL_SPEED_SQUARED = 0.015D;
    private static final double MAXIMUM_TARGET_VERTICAL_SPEED = 0.14D;
    private static final double MAXIMUM_ACQUISITION_DISTANCE_SQUARED = 36D;
    private static final double BASE_HORIZONTAL_TARGET_EXPANSION = 0.45D;
    private static final double BASE_VERTICAL_TARGET_EXPANSION = 0.15D;

    private Targeting()
    {
        super("Targeting",
              TargetingNoise.INSTANCE,
              TargetingPrecision.INSTANCE,
              TargetingPattern.INSTANCE,
              TargetingSwitching.INSTANCE,
              TargetingMixed.INSTANCE,
              TargetingReplay.INSTANCE,
              TargetingAcquisition.INSTANCE,
              TargetingSilentRotation.INSTANCE);
    }

    /**
     * Uses scaffold-like horizontal placements as interaction points for the shared targeting analysis.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event)
    {
        final var user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        final BlockFace face = event.getBlockPlaced().getFace(event.getBlockAgainst());
        final var playerLocation = event.getPlayer().getLocation();
        final var blockLocation = event.getBlockPlaced().getLocation();

        // Restrict the scaffold context to nearby horizontal placements below the player. Ordinary building creates
        // unrelated rotations and would make the interaction samples substantially less meaningful.
        if (face == null ||
            !WorldUtil.HORIZONTAL_FACES.contains(face) ||
            playerLocation.getY() <= blockLocation.getY() ||
            playerLocation.distanceSquared(blockLocation) > 16D) return;

        analyze(user, TargetingContext.SCAFFOLD);
    }

    /**
     * Records a server-confirmed camera-context change without inserting a synthetic statistical sample. The next real
     * client movement packet receives a trusted boundary, while all earlier samples and evidence remain available.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(final PlayerTeleportEvent event)
    {
        markTrustedBoundary(event.getPlayer(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(final PlayerRespawnEvent event)
    {
        markTrustedBoundary(event.getPlayer(), event.getRespawnLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(final PlayerChangedWorldEvent event)
    {
        markTrustedBoundary(event.getPlayer(), event.getPlayer().getLocation());
    }

    private void markTrustedBoundary(final Player player, final Location destination)
    {
        final User user = User.getUser(player);
        if (user == null || destination == null) return;

        user.getTargetingData().addTrustedMovement(destination.getX(),
                                                   destination.getY(),
                                                   destination.getZ(),
                                                   destination.getYaw(),
                                                   destination.getPitch(),
                                                   System.nanoTime());
    }

    /**
     * Builds a conservative target-relative acquisition sample from a successful player hit.
     *
     * <p>The check deliberately ignores fast-moving targets, vehicles, and flight. Without full client-side entity
     * rewind those situations make target geometry too uncertain for a low-false-positive slowdown detector. This is
     * an eligibility restriction for Acquisition only; it does not clear or weaken the other Targeting submodules.</p>
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageByEntityEvent event)
    {
        if (!(event.getDamager() instanceof Player attacker) ||
            !(event.getEntity() instanceof Player target)) return;

        final var user = User.getUser(attacker);
        if (User.isUserInvalid(user, this) ||
            !TargetingAcquisition.INSTANCE.isEnabled() ||
            User.isUserInvalid(user, TargetingAcquisition.INSTANCE) ||
            !user.inAdventureOrSurvivalMode() ||
            attacker.isInsideVehicle() ||
            attacker.isFlying() ||
            EntityUtil.INSTANCE.isFlyingWithElytra(attacker) ||
            attacker.getWorld() != target.getWorld()) return;

        final var attackerLocation = attacker.getLocation();
        final var targetLocation = target.getLocation();
        if (attackerLocation.distanceSquared(targetLocation) > MAXIMUM_ACQUISITION_DISTANCE_SQUARED) return;

        final var targetVelocity = target.getVelocity();
        final double horizontalSpeedSquared = targetVelocity.getX() * targetVelocity.getX() +
                                              targetVelocity.getZ() * targetVelocity.getZ();
        if (horizontalSpeedSquared > MAXIMUM_TARGET_HORIZONTAL_SPEED_SQUARED ||
            Math.abs(targetVelocity.getY()) > MAXIMUM_TARGET_VERTICAL_SPEED) return;

        final long currentTimestamp = System.nanoTime();
        if (user.getData().object.packetFloodData.isThrottled(currentTimestamp)) return;

        user.getTargetingData().takeAcquisitionSnapshot(currentTimestamp).ifPresent(snapshot -> {
            final double motionExpansion = Math.min(0.18D, Math.sqrt(horizontalSpeedSquared) * 1.5D);
            final double horizontalExpansion = BASE_HORIZONTAL_TARGET_EXPANSION + motionExpansion;
            final double verticalExpansion = BASE_VERTICAL_TARGET_EXPANSION +
                                             Math.min(0.12D, Math.abs(targetVelocity.getY()));
            final double targetHeight = Math.max(1.5D, target.getEyeHeight() + 0.3D);
            final TargetingAcquisitionAnalysis.TargetBox targetBox = new TargetingAcquisitionAnalysis.TargetBox(
                    targetLocation.getX() - horizontalExpansion,
                    targetLocation.getY() - verticalExpansion,
                    targetLocation.getZ() - horizontalExpansion,
                    targetLocation.getX() + horizontalExpansion,
                    targetLocation.getY() + targetHeight + verticalExpansion,
                    targetLocation.getZ() + horizontalExpansion);
            final TargetingAcquisitionAnalysis.Result result = TargetingAcquisitionAnalysis.analyze(snapshot,
                                                                                                    targetBox,
                                                                                                    attacker.getEyeHeight());
            if (result.valid()) TargetingAcquisition.INSTANCE.analyze(user, result.profile());
        });
    }

    /**
     * Collects every movement packet and observes attack packets. PacketEvents 2.13 separates attacks into ATTACK on
     * Minecraft 26.1+, while older client versions still use INTERACT_ENTITY.
     */
    @Override
    public ModuleLoader createModuleLoader()
    {
        final var packetAdapter = PacketAdapterBuilder
                .of(this,
                    PacketType.Play.Client.PLAYER_FLYING,
                    PacketType.Play.Client.PLAYER_POSITION,
                    PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION,
                    PacketType.Play.Client.PLAYER_ROTATION,
                    PacketType.Play.Client.INTERACT_ENTITY,
                    PacketType.Play.Client.ATTACK)
                .priority(PacketListenerPriority.LOW)
                .onReceiving((event, user) -> {
                    if (User.isUserInvalid(user, this)) return;

                    final var packetType = event.getPacketType();
                    if (isMovementPacket(packetType)) {
                        final long currentTimestamp = System.nanoTime();
                        final var wrapper = new WrapperPlayClientPlayerFlying(event);
                        final var location = wrapper.getLocation();
                        final TargetingData.RotationUpdate update = user.getTargetingData().addMovement(
                                location.getX(),
                                location.getY(),
                                location.getZ(),
                                location.getYaw(),
                                location.getPitch(),
                                wrapper.hasPositionChanged(),
                                wrapper.hasRotationChanged(),
                                currentTimestamp);

                        if (!update.accepted()) return;
                        return;
                    }

                    if (packetType == PacketType.Play.Client.ATTACK) {
                        analyze(user, TargetingContext.COMBAT);
                        return;
                    }

                    final var wrapper = new WrapperPlayClientInteractEntity(event);
                    if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                        analyze(user, TargetingContext.COMBAT);
                    }
                }).build();

        return ModuleLoader.builder(this)
                           .addPacketListeners(packetAdapter)
                           .build();
    }

    private void analyze(final User user, final TargetingContext context)
    {
        // The server has changed the camera context, but no client movement packet has acknowledged that orientation
        // yet. Keep all accumulated evidence, then resume on the first real post-teleport sample.
        final long currentTimestamp = System.nanoTime();
        if (user.getTargetingData().hasPendingTrustedBoundary() ||
            user.getTargetingData().isTargetingSuppressed(currentTimestamp) ||
            user.getData().object.packetFloodData.isThrottled(currentTimestamp)) return;

        user.getTargetingData().takeSnapshot().ifPresent(snapshot -> {
            final double[] yaw = snapshot.yaw();
            final double[] pitch = snapshot.pitch();
            final TargetingAnalysis.Result result = TargetingAnalysis.analyze(yaw,
                                                                              pitch,
                                                                              snapshot.trustedBreakBefore());

            final boolean noiseActive = TargetingNoise.INSTANCE.isEnabled() &&
                                        !User.isUserInvalid(user, TargetingNoise.INSTANCE);
            final boolean precisionActive = TargetingPrecision.INSTANCE.isEnabled() &&
                                            !User.isUserInvalid(user, TargetingPrecision.INSTANCE);
            final boolean patternActive = TargetingPattern.INSTANCE.isEnabled() &&
                                          !User.isUserInvalid(user, TargetingPattern.INSTANCE);
            final boolean switchingActive = TargetingSwitching.INSTANCE.isEnabled() &&
                                            !User.isUserInvalid(user, TargetingSwitching.INSTANCE);
            final TargetingSwitchAnalysis.Result switchResult = switchingActive
                                                                ? TargetingSwitchAnalysis.analyze(result)
                                                                : null;

            if (noiseActive) TargetingNoise.INSTANCE.analyze(user, context, result);
            if (precisionActive) TargetingPrecision.INSTANCE.analyze(user, context, result);
            if (patternActive) TargetingPattern.INSTANCE.analyze(user, context, result);
            if (switchingActive) TargetingSwitching.INSTANCE.analyze(user, context, switchResult);

            if (TargetingReplay.INSTANCE.isEnabled() && !User.isUserInvalid(user, TargetingReplay.INSTANCE)) {
                final TargetingReplayData.ReplayResult replayResult = user.getTargetingReplayData()
                                                                          .compareAndRemember(context,
                                                                                              snapshot.firstSequence(),
                                                                                              snapshot.lastSequence(),
                                                                                              result);
                TargetingReplay.INSTANCE.analyze(user, context, replayResult);
            }

            if (TargetingMixed.INSTANCE.isEnabled() && !User.isUserInvalid(user, TargetingMixed.INSTANCE)) {
                final int modeMask = TargetingMixedAnalysis.modeMask(
                        noiseActive && result.randomizedAxisCount() > 0,
                        precisionActive && TargetingPrecision.isSuspicious(result),
                        patternActive && result.syntheticAxisCount() > 0,
                        switchingActive && switchResult.switchingAxisCount() > 0);
                final int[] modeHistory = user.getTargetingData().addMixedModeObservation(context, modeMask);
                TargetingMixed.INSTANCE.analyze(user, context, TargetingMixedAnalysis.analyze(modeHistory));
            }
        });
    }

    private static boolean isMovementPacket(final Object packetType)
    {
        return packetType == PacketType.Play.Client.PLAYER_FLYING ||
               packetType == PacketType.Play.Client.PLAYER_POSITION ||
               packetType == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION ||
               packetType == PacketType.Play.Client.PLAYER_ROTATION;
    }

    @Override
    public ViolationManagement createViolationManagement()
    {
        return new ViolationAggregation(this,
                                        ThresholdManagement.loadThresholds(this),
                                        getChildren().stream()
                                                     .map(ViolationModule.class::cast)
                                                     .map(ViolationModule::getManagement)
                                                     .collect(Collectors.toUnmodifiableSet()));
    }
}
