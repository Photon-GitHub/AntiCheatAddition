package de.photon.anticheataddition.modules.checks.targeting;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.subdata.TargetingData;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

import java.util.Locale;

/**
 * Detects an interaction-only rotation which is restored through a direct subsequent return path.
 *
 * <p>This covers silent aim and scaffold rotations which briefly point the server at a target while preserving the
 * player's visible camera direction. There is no receive-time or fixed packet-count cutoff; instead, the return must
 * remain sufficiently direct. One axis is sufficient, but a matching restoration on both axes receives more
 * evidence and VL.</p>
 */
public final class TargetingSnapBack extends ViolationModule
{
    public static final TargetingSnapBack INSTANCE = new TargetingSnapBack();

    private static final int SINGLE_AXIS_ADDED_VL = 12;
    private static final int BOTH_AXES_ADDED_VL = 24;

    private TargetingSnapBack()
    {
        super("Targeting.parts.SnapBack");
    }

    /**
     * Processes a completed interaction and restoration cycle.
     */
    public void analyze(final User user, final TargetingData.SnapBackSample sample)
    {
        final var counter = user.getData().counter.targetingSnapBackFails;
        if (sample == null || sample.suspiciousAxes() == 0) {
            counter.decrementAboveZero();
            return;
        }

        if (!counter.incrementCompareThreshold(sample.suspiciousAxes())) return;
        getManagement().flag(Flag.of(user)
                                 .setAddedVl(sample.suspiciousAxes() == 2 ? BOTH_AXES_ADDED_VL : SINGLE_AXIS_ADDED_VL)
                                 .setDebug(() -> debugMessage(user, sample)));
    }

    private static String debugMessage(final User user, final TargetingData.SnapBackSample sample)
    {
        return String.format(Locale.ROOT,
                             "TargetingData-Debug | Player: %s restored an interaction-only rotation " +
                             "(context: %s, axes: %d, yaw_snap: %.4f, yaw_return: %.4f, yaw_error: %.5f, " +
                             "pitch_snap: %.4f, pitch_return: %.4f, pitch_error: %.5f, delay_ms: %.2f, packets: %d)",
                             user.getPlayer().getName(),
                             sample.context(),
                             sample.suspiciousAxes(),
                             sample.yawSnap(),
                             sample.yawReturn(),
                             sample.yawReturnError(),
                             sample.pitchSnap(),
                             sample.pitchReturn(),
                             sample.pitchReturnError(),
                             sample.delayNanos() / 1_000_000D,
                             sample.followingPackets());
    }

    @Override
    public ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(300, 2).build();
    }
}
