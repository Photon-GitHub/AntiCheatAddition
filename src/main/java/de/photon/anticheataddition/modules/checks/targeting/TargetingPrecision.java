package de.photon.anticheataddition.modules.checks.targeting;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

import java.util.Locale;

/**
 * Detects targeting windows with effectively no residual variation on one or both axes.
 *
 * <p>This is intentionally more buffered than the Noise submodule. A human can briefly keep one axis still, whereas a
 * repeated sequence of mathematically smooth or constant targeting windows is substantially more suspicious.</p>
 */
public final class TargetingPrecision extends ViolationModule
{
    public static final TargetingPrecision INSTANCE = new TargetingPrecision();

    private static final int SINGLE_AXIS_ADDED_VL = 8;
    private static final int BOTH_AXES_ADDED_VL = 16;
    private static final double MINIMUM_COMBINED_ROTATION_RANGE = 0.5D;
    private static final double ACTIVE_AXIS_ROTATION_RANGE = 0.15D;

    private TargetingPrecision()
    {
        super("Targeting.parts.Precision");
    }

    /**
     * Processes a result produced by the shared Targeting parent module.
     */
    public void analyze(final User user,
                        final TargetingContext context,
                        final TargetingAnalysis.Result result)
    {
        final int preciseAxes = result.preciseAxisCount();
        final var counter = user.getData().counter.targetingPrecisionFails;

        // A completely stationary camera is not evidence of targeting a fixed point. At least one axis must be
        // actively tracking before a zero-residual axis is considered suspicious.
        if (!isSuspicious(result)) {
            counter.decrementAboveZero();
            return;
        }

        // A constant axis contributes one point. A mathematically exact but actively changing axis contributes two,
        // because it cannot be explained merely by the player not touching that mouse direction for a moment.
        final int evidence = precisionEvidence(result.yaw()) + precisionEvidence(result.pitch());
        if (!counter.incrementCompareThreshold(evidence)) return;

        final int addedVl = preciseAxes == 2 ? BOTH_AXES_ADDED_VL : SINGLE_AXIS_ADDED_VL;
        getManagement().flag(Flag.of(user)
                                 .setAddedVl(addedVl)
                                 .setDebug(() -> debugMessage(user, context, result, preciseAxes, evidence)));
    }

    /**
     * Returns whether the shared result contains precision evidence strong enough for this submodule.
     */
    public static boolean isSuspicious(final TargetingAnalysis.Result result)
    {
        if (result == null) throw new NullPointerException("result must not be null");
        return result.preciseAxisCount() > 0 &&
               result.yaw().rotationRange() + result.pitch().rotationRange() >= MINIMUM_COMBINED_ROTATION_RANGE;
    }

    private static int precisionEvidence(final TargetingAnalysis.AxisResult axis)
    {
        if (axis.pattern() != TargetingAnalysis.Pattern.PRECISE) return 0;
        return axis.rotationRange() >= ACTIVE_AXIS_ROTATION_RANGE ? 2 : 1;
    }

    private static String debugMessage(final User user,
                                       final TargetingContext context,
                                       final TargetingAnalysis.Result result,
                                       final int preciseAxes,
                                       final int evidence)
    {
        return String.format(Locale.ROOT,
                             "TargetingData-Debug | Player: %s sent excessively precise targeting rotations " +
                             "(context: %s, axes: %d, evidence: %d, yaw_pattern: %s, yaw_sd: %.8f, yaw_max_residual: %.8f, " +
                             "yaw_range: %.4f, pitch_pattern: %s, pitch_sd: %.8f, pitch_max_residual: %.8f, " +
                             "pitch_range: %.4f)",
                             user.getPlayer().getName(),
                             context,
                             preciseAxes,
                             evidence,
                             result.yaw().pattern(),
                             result.yaw().standardDeviation(),
                             result.yaw().maxAbsoluteResidual(),
                             result.yaw().rotationRange(),
                             result.pitch().pattern(),
                             result.pitch().standardDeviation(),
                             result.pitch().maxAbsoluteResidual(),
                             result.pitch().rotationRange());
    }

    @Override
    public ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(300, 2).build();
    }
}
