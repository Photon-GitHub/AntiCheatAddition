package de.photon.anticheataddition.modules.checks.targeting;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

import java.util.Locale;

/**
 * Detects repeated, oscillating large rotations used to invalidate ordinary targeting windows.
 *
 * <p>One large turn is ignored. Evidence requires several robust discontinuities which repeatedly change direction and
 * cancel most of their travelled path. There is no maximum rotation exemption.</p>
 */
public final class TargetingDiscontinuity extends ViolationModule
{
    public static final TargetingDiscontinuity INSTANCE = new TargetingDiscontinuity();

    private static final int SINGLE_AXIS_ADDED_VL = 10;
    private static final int BOTH_AXES_ADDED_VL = 20;

    private TargetingDiscontinuity()
    {
        super("Targeting.parts.Discontinuity");
    }

    /**
     * Processes one shared Targeting window.
     */
    public void analyze(final User user,
                        final TargetingContext context,
                        final TargetingDiscontinuityAnalysis.Result result)
    {
        final int suspiciousAxes = result.suspiciousAxisCount();
        final var counter = user.getData().counter.targetingDiscontinuityFails;
        if (suspiciousAxes == 0) {
            counter.decrementAboveZero();
            return;
        }

        if (!counter.incrementCompareThreshold(result.evidenceWeight())) return;
        getManagement().flag(Flag.of(user)
                                 .setAddedVl(suspiciousAxes == 2 ? BOTH_AXES_ADDED_VL : SINGLE_AXIS_ADDED_VL)
                                 .setDebug(() -> debugMessage(user, context, result)));
    }

    private static String debugMessage(final User user,
                                       final TargetingContext context,
                                       final TargetingDiscontinuityAnalysis.Result result)
    {
        return String.format(Locale.ROOT,
                             "TargetingData-Debug | Player: %s sent repeated oscillating rotation discontinuities " +
                             "(context: %s, axes: %d, weight: %d, yaw_count: %d, yaw_changes: %.3f, " +
                             "yaw_efficiency: %.3f, yaw_reversal: %.3f, yaw_max: %.3f, yaw_windows: %d, " +
                             "pitch_count: %d, pitch_changes: %.3f, pitch_efficiency: %.3f, " +
                             "pitch_reversal: %.3f, pitch_max: %.3f, pitch_windows: %d)",
                             user.getPlayer().getName(),
                             context,
                             result.suspiciousAxisCount(),
                             result.evidenceWeight(),
                             result.yaw().discontinuityCount(),
                             result.yaw().directionChangeRatio(),
                             result.yaw().pathEfficiency(),
                             result.yaw().reversalEnergy(),
                             result.yaw().maximumDelta(),
                             result.yaw().suspiciousWindowCount(),
                             result.pitch().discontinuityCount(),
                             result.pitch().directionChangeRatio(),
                             result.pitch().pathEfficiency(),
                             result.pitch().reversalEnergy(),
                             result.pitch().maximumDelta(),
                             result.pitch().suspiciousWindowCount());
    }

    @Override
    public ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(300, 2).build();
    }
}
