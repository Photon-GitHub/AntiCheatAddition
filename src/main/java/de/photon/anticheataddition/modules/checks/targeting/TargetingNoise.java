package de.photon.anticheataddition.modules.checks.targeting;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

import java.util.Locale;

/**
 * Detects weakly correlated randomized residuals on either targeting axis.
 *
 * <p>Gaussian and uniform distributions are identified explicitly. A distribution-free branch additionally covers
 * triangular, Laplace-like, clipped, bimodal, and low-discrepancy generators when their temporal behavior is still too
 * random for ordinary mouse movement. A single randomized axis is sufficient. Two independent randomized axes
 * accumulate evidence twice as quickly and add more VL.</p>
 */
public final class TargetingNoise extends ViolationModule
{
    public static final TargetingNoise INSTANCE = new TargetingNoise();

    private static final int SINGLE_AXIS_ADDED_VL = 10;
    private static final int BOTH_AXES_ADDED_VL = 20;
    private static final double MAX_DOUBLE_AXIS_CROSS_CORRELATION = 0.35D;

    private TargetingNoise()
    {
        super("Targeting.parts.Noise");
    }

    /**
     * Processes a result produced by the shared Targeting parent module.
     */
    public void analyze(final User user,
                        final TargetingContext context,
                        final TargetingAnalysis.Result result)
    {
        final int randomizedAxes = result.randomizedAxisCount();
        final var counter = user.getData().counter.targetingNoiseFails;

        if (randomizedAxes == 0) {
            counter.decrementAboveZero();
            return;
        }

        // One randomized axis is already sufficient. The stronger two-axis weighting is only used when both residual
        // streams are reasonably independent, as correlated yaw and pitch can also be caused by intentional movement.
        final boolean independentDoubleAxis = randomizedAxes == 2 &&
                                              Math.abs(result.crossCorrelation()) <= MAX_DOUBLE_AXIS_CROSS_CORRELATION;
        final int evidence = independentDoubleAxis ? 2 : 1;
        if (!counter.incrementCompareThreshold(evidence)) return;

        final int addedVl = independentDoubleAxis ? BOTH_AXES_ADDED_VL : SINGLE_AXIS_ADDED_VL;
        getManagement().flag(Flag.of(user)
                                 .setAddedVl(addedVl)
                                 .setDebug(() -> debugMessage(user,
                                                              context,
                                                              result,
                                                              randomizedAxes,
                                                              independentDoubleAxis)));
    }

    private static String debugMessage(final User user,
                                       final TargetingContext context,
                                       final TargetingAnalysis.Result result,
                                       final int randomizedAxes,
                                       final boolean independentDoubleAxis)
    {
        return String.format(Locale.ROOT,
                             "TargetingData-Debug | Player: %s sent randomized targeting residuals " +
                             "(context: %s, axes: %d, independent_double_axis: %s, " +
                             "yaw_pattern: %s, yaw_sd: %.5f, yaw_score: %.4f, yaw_ac: %.4f, yaw_ks_p: %.4f, " +
                             "yaw_entropy: %.4f, yaw_runs_z: %.4f, yaw_windows: %d, " +
                             "pitch_pattern: %s, pitch_sd: %.5f, pitch_score: %.4f, pitch_ac: %.4f, pitch_ks_p: %.4f, " +
                             "pitch_entropy: %.4f, pitch_runs_z: %.4f, pitch_windows: %d, cross_corr: %.4f)",
                             user.getPlayer().getName(),
                             context,
                             randomizedAxes,
                             independentDoubleAxis,
                             result.yaw().pattern(),
                             result.yaw().standardDeviation(),
                             result.yaw().randomnessScore(),
                             result.yaw().averageAbsAutocorrelation(),
                             result.yaw().ksPValue(),
                             result.yaw().permutationEntropy(),
                             result.yaw().runsZScore(),
                             result.yaw().randomWindowCount(),
                             result.pitch().pattern(),
                             result.pitch().standardDeviation(),
                             result.pitch().randomnessScore(),
                             result.pitch().averageAbsAutocorrelation(),
                             result.pitch().ksPValue(),
                             result.pitch().permutationEntropy(),
                             result.pitch().runsZScore(),
                             result.pitch().randomWindowCount(),
                             result.crossCorrelation());
    }

    @Override
    public ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(200, 2).build();
    }
}
