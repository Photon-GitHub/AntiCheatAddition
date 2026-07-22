package de.photon.anticheataddition.modules.checks.targeting;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.subdata.TargetingReplayData;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

import java.util.Locale;

/**
 * Detects replayed or repeatedly seeded targeting traces.
 *
 * <p>A client can record a human-looking correction sequence and replay it instead of generating obvious noise. The
 * normalized residual fingerprint ignores absolute angle, amplitude, mirroring, small phase offsets, and time reversal.
 * Only non-overlapping interaction windows are compared, so the ordinary overlap between consecutive snapshots cannot
 * produce a detection.</p>
 */
public final class TargetingReplay extends ViolationModule
{
    public static final TargetingReplay INSTANCE = new TargetingReplay();

    private static final int SINGLE_AXIS_ADDED_VL = 14;
    private static final int BOTH_AXES_ADDED_VL = 28;

    private TargetingReplay()
    {
        super("Targeting.parts.Replay");
    }

    /**
     * Processes one replay comparison produced for an interaction window.
     */
    public void analyze(final User user,
                        final TargetingContext context,
                        final TargetingReplayData.ReplayResult result)
    {
        final var counter = user.getData().counter.targetingReplayFails;
        if (result.repeatedAxes() == 0) {
            counter.decrementAboveZero();
            return;
        }

        if (!counter.incrementCompareThreshold(result.repeatedAxes())) return;
        getManagement().flag(Flag.of(user)
                                 .setAddedVl(result.repeatedAxes() == 2 ? BOTH_AXES_ADDED_VL : SINGLE_AXIS_ADDED_VL)
                                 .setDebug(() -> debugMessage(user, context, result)));
    }

    private static String debugMessage(final User user,
                                       final TargetingContext context,
                                       final TargetingReplayData.ReplayResult result)
    {
        return String.format(Locale.ROOT,
                             "TargetingData-Debug | Player: %s repeated a normalized targeting trace " +
                             "(context: %s, axes: %d, yaw_corr: %.5f, yaw_error: %.5f, yaw_reversed: %s, " +
                             "pitch_corr: %.5f, pitch_error: %.5f, pitch_reversed: %s)",
                             user.getPlayer().getName(),
                             context,
                             result.repeatedAxes(),
                             result.yawCorrelation(),
                             result.yawError(),
                             result.yawReversed(),
                             result.pitchCorrelation(),
                             result.pitchError(),
                             result.pitchReversed());
    }

    @Override
    public ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(400, 2).build();
    }
}
