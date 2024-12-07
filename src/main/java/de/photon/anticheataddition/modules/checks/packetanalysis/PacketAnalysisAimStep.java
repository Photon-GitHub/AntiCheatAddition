package de.photon.anticheataddition.modules.checks.packetanalysis;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.minecraft.world.entity.EntityUtil;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class PacketAnalysisAimStep extends ViolationModule implements Listener
{
    public static final PacketAnalysisAimStep INSTANCE = new PacketAnalysisAimStep();
    private static final double NO_MOVE_DELTA_THRESHOLD = 0.00001D;
    private static final double STEP_MOVE_DELTA_THRESHOLD = 1D;

    private PacketAnalysisAimStep()
    {
        super("PacketAnalysis.parts.AimStep");
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event)
    {
        final var user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this)
            || event.getTo() == null
            || user.getPlayer().isInsideVehicle()
            // Not flying (vanilla or elytra) as it may trigger some fps
            || user.getPlayer().isFlying()
            || EntityUtil.INSTANCE.isFlyingWithElytra(user.getPlayer())) return;

        final double yawDelta = MathUtil.yawDistance(event.getTo().getYaw(), event.getFrom().getYaw());
        final double pitchDelta = MathUtil.absDiff(event.getTo().getPitch(), event.getFrom().getPitch());

        // False positives as the pitch is capped if a player looks straight up or down.
        if (pitchDelta < NO_MOVE_DELTA_THRESHOLD && (event.getTo().getPitch() == 90 || event.getTo().getPitch() == -90)) return;

        if (user.getData().counter.packetAnalysisAimStepFails.conditionallyIncDec(isAimStep(yawDelta, pitchDelta) || isAimStep(pitchDelta, yawDelta))) {
            getManagement().flag(Flag.of(user).setAddedVl(20).setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " sent step-like aim movements (d_yaw: " + yawDelta + ", d_pitch: " + pitchDelta + ")"));
        }
    }

    private static boolean isAimStep(double deltaOne, double deltaTwo)
    {
        return deltaOne < NO_MOVE_DELTA_THRESHOLD && deltaTwo > STEP_MOVE_DELTA_THRESHOLD;
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(200, 2).build();
    }
}
