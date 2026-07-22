package de.photon.anticheataddition.modules.checks.targeting;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

import java.util.Locale;

/**
 * Detects repeated large opposing rotations which end exactly at an interaction.
 *
 * <p>This covers attempts to poison statistical history by looking far away, including towards the opposite direction,
 * immediately before rotating to the real target. One isolated reversal is buffered because legitimate players can make
 * a poor correction; repeated interaction-aligned reversals are substantially less plausible.</p>
 */
public final class TargetingReversal extends ViolationModule
{
    public static final TargetingReversal INSTANCE = new TargetingReversal();

    private static final int SINGLE_AXIS_ADDED_VL = 10;
    private static final int BOTH_AXES_ADDED_VL = 20;

    private TargetingReversal()
    {
        super("Targeting.parts.Reversal");
    }

    /**
     * Processes one interaction-centered reversal analysis.
     */
    public void analyze(final User user, final TargetingReversalAnalysis.Result result)
    {
        final int suspiciousAxes = result.suspiciousAxisCount();
        final var counter = user.getData().counter.targetingReversalFails;
        if (suspiciousAxes == 0) {
            counter.decrementAboveZero();
            return;
        }

        if (!counter.incrementCompareThreshold(result.evidenceWeight())) return;
        getManagement().flag(Flag.of(user)
                                 .setAddedVl(suspiciousAxes == 2 ? BOTH_AXES_ADDED_VL : SINGLE_AXIS_ADDED_VL)
                                 .setDebug(() -> debugMessage(user, result)));
    }

    private static String debugMessage(final User user, final TargetingReversalAnalysis.Result result)
    {
        return String.format(Locale.ROOT,
                             "TargetingData-Debug | Player: %s sent an interaction-aligned abrupt reversal " +
                             "(context: %s, axes: %d, weight: %d, yaw_suspicious: %s, yaw_severe: %s, " +
                             "yaw_first: %.3f, yaw_return: %.3f, yaw_cancel: %.3f, yaw_relative: %.3f, " +
                             "yaw_packets: %d, pitch_suspicious: %s, pitch_severe: %s, pitch_first: %.3f, " +
                             "pitch_return: %.3f, pitch_cancel: %.3f, pitch_relative: %.3f, pitch_packets: %d)",
                             user.getPlayer().getName(),
                             result.context(),
                             result.suspiciousAxisCount(),
                             result.evidenceWeight(),
                             result.yaw().suspicious(),
                             result.yaw().severe(),
                             result.yaw().earlierMovement(),
                             result.yaw().recentMovement(),
                             result.yaw().cancellationRatio(),
                             result.yaw().relativeStrength(),
                             result.yaw().packetCount(),
                             result.pitch().suspicious(),
                             result.pitch().severe(),
                             result.pitch().earlierMovement(),
                             result.pitch().recentMovement(),
                             result.pitch().cancellationRatio(),
                             result.pitch().relativeStrength(),
                             result.pitch().packetCount());
    }

    @Override
    public ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(300, 2).build();
    }
}
