package de.photon.anticheataddition.modules.checks.targeting;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

import java.util.Locale;

/**
 * Detects deterministic residual generators used in place of Gaussian or uniform noise.
 *
 * <p>Common examples are sine waves, short repeating tables, alternating offsets, and low-cardinality sequences. These
 * patterns can have a non-random marginal distribution and therefore deliberately avoid a conventional KS-style noise
 * check. A single patterned axis is sufficient, while independent patterns on both axes receive greater weight.</p>
 */
public final class TargetingPattern extends ViolationModule
{
    public static final TargetingPattern INSTANCE = new TargetingPattern();

    private static final int SINGLE_AXIS_ADDED_VL = 8;
    private static final int BOTH_AXES_ADDED_VL = 16;
    private static final double MAX_DOUBLE_AXIS_CROSS_CORRELATION = 0.35D;

    private TargetingPattern()
    {
        super("Targeting.parts.Pattern");
    }

    /**
     * Processes a result produced by the shared Targeting parent module.
     */
    public void analyze(final User user,
                        final TargetingContext context,
                        final TargetingAnalysis.Result result)
    {
        final int syntheticAxes = result.syntheticAxisCount();
        final var counter = user.getData().counter.targetingPatternFails;
        if (syntheticAxes == 0) {
            counter.decrementAboveZero();
            return;
        }

        final boolean independentDoubleAxis = syntheticAxes == 2 &&
                                              Math.abs(result.crossCorrelation()) <= MAX_DOUBLE_AXIS_CROSS_CORRELATION;
        final int evidence = independentDoubleAxis ? 2 : 1;
        if (!counter.incrementCompareThreshold(evidence)) return;

        getManagement().flag(Flag.of(user)
                                 .setAddedVl(independentDoubleAxis ? BOTH_AXES_ADDED_VL : SINGLE_AXIS_ADDED_VL)
                                 .setDebug(() -> debugMessage(user,
                                                              context,
                                                              result,
                                                              syntheticAxes,
                                                              independentDoubleAxis)));
    }

    private static String debugMessage(final User user,
                                       final TargetingContext context,
                                       final TargetingAnalysis.Result result,
                                       final int syntheticAxes,
                                       final boolean independentDoubleAxis)
    {
        return String.format(Locale.ROOT,
                             "TargetingData-Debug | Player: %s sent deterministic targeting residuals " +
                             "(context: %s, axes: %d, independent_double_axis: %s, " +
                             "yaw_pattern: %s, yaw_period_corr: %.4f, yaw_period: %d, yaw_repeat_error: %.4f, " +
                             "yaw_entropy: %.4f, yaw_levels: %.4f, yaw_windows: %d, " +
                             "pitch_pattern: %s, pitch_period_corr: %.4f, pitch_period: %d, " +
                             "pitch_repeat_error: %.4f, pitch_entropy: %.4f, pitch_levels: %.4f, pitch_windows: %d)",
                             user.getPlayer().getName(),
                             context,
                             syntheticAxes,
                             independentDoubleAxis,
                             result.yaw().syntheticPattern(),
                             result.yaw().maxPeriodicCorrelation(),
                             result.yaw().periodicLag(),
                             result.yaw().repeatError(),
                             result.yaw().permutationEntropy(),
                             result.yaw().distinctLevelRatio(),
                             result.yaw().syntheticWindowCount(),
                             result.pitch().syntheticPattern(),
                             result.pitch().maxPeriodicCorrelation(),
                             result.pitch().periodicLag(),
                             result.pitch().repeatError(),
                             result.pitch().permutationEntropy(),
                             result.pitch().distinctLevelRatio(),
                             result.pitch().syntheticWindowCount());
    }

    @Override
    public ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(300, 2).build();
    }
}
