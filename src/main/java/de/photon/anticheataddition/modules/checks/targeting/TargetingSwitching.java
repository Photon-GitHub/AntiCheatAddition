package de.photon.anticheataddition.modules.checks.targeting;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

import java.util.Locale;

/**
 * Detects repeated switching between clean and randomized targeting sections.
 *
 * <p>This covers clients which poison a statistical window with clean packets, enable randomization only shortly before
 * an interaction, or alternate the randomized axis. The subcheck is heavily buffered because human corrections can
 * contain an isolated burst, while a client repeats the same variance switching over many interactions.</p>
 */
public final class TargetingSwitching extends ViolationModule
{
    public static final TargetingSwitching INSTANCE = new TargetingSwitching();

    private static final int SINGLE_AXIS_ADDED_VL = 6;
    private static final int BOTH_AXES_ADDED_VL = 12;

    private TargetingSwitching()
    {
        super("Targeting.parts.Switching");
    }

    /**
     * Processes one segmented-variance analysis.
     */
    public void analyze(final User user,
                        final TargetingContext context,
                        final TargetingSwitchAnalysis.Result result)
    {
        final int switchingAxes = result.switchingAxisCount();
        final var counter = user.getData().counter.targetingSwitchingFails;
        if (switchingAxes == 0) {
            counter.decrementAboveZero();
            return;
        }

        if (!counter.incrementCompareThreshold(switchingAxes)) return;
        getManagement().flag(Flag.of(user)
                                 .setAddedVl(switchingAxes == 2 ? BOTH_AXES_ADDED_VL : SINGLE_AXIS_ADDED_VL)
                                 .setDebug(() -> debugMessage(user, context, result, switchingAxes)));
    }

    private static String debugMessage(final User user,
                                       final TargetingContext context,
                                       final TargetingSwitchAnalysis.Result result,
                                       final int switchingAxes)
    {
        return String.format(Locale.ROOT,
                             "TargetingData-Debug | Player: %s repeatedly switched targeting variance " +
                             "(context: %s, axes: %d, yaw_contrast: %.3f, yaw_min: %.3f, yaw_max: %.3f, " +
                             "yaw_active: %d, yaw_quiet: %d, yaw_transitions: %d, pitch_contrast: %.3f, " +
                             "pitch_min: %.3f, pitch_max: %.3f, pitch_active: %d, pitch_quiet: %d, " +
                             "pitch_transitions: %d)",
                             user.getPlayer().getName(),
                             context,
                             switchingAxes,
                             result.yaw().varianceContrast(),
                             result.yaw().minimumSegmentRms(),
                             result.yaw().maximumSegmentRms(),
                             result.yaw().activeSegments(),
                             result.yaw().quietSegments(),
                             result.yaw().stateTransitions(),
                             result.pitch().varianceContrast(),
                             result.pitch().minimumSegmentRms(),
                             result.pitch().maximumSegmentRms(),
                             result.pitch().activeSegments(),
                             result.pitch().quietSegments(),
                             result.pitch().stateTransitions());
    }

    @Override
    public ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(400, 2).build();
    }
}
