package de.photon.anticheataddition.modules.checks.targeting;

import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.minecraft.world.entity.EntityUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Collects conservative successful-hit geometry for the Acquisition subcheck on the server thread. */
final class TargetingAcquisitionListener implements Listener
{
    private static final double MAXIMUM_TARGET_HORIZONTAL_SPEED_SQUARED = 0.015D;
    private static final double MAXIMUM_TARGET_VERTICAL_SPEED = 0.14D;
    private static final double MAXIMUM_ACQUISITION_DISTANCE_SQUARED = 36D;
    private static final double BASE_HORIZONTAL_TARGET_EXPANSION = 0.45D;
    private static final double BASE_VERTICAL_TARGET_EXPANSION = 0.15D;

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
        if (User.isUserInvalid(user, Targeting.INSTANCE) ||
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
}
