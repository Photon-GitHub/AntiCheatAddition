package de.photon.anticheataddition.modules.checks.targeting;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

import java.util.Locale;

/**
 * Detects repeated switching between different suspicious targeting strategies across complete analysis windows.
 *
 * <p>This does not turn a merely unusual window into a violation. It only combines classifications already produced by
 * enabled Targeting submodules and requires several suspicious observations with repeated category changes. The extra
 * evidence buffer is intentionally conservative because the purpose is to close counter-splitting bypasses, not to
 * replace the more specific checks.</p>
 */
public final class TargetingMixed extends ViolationModule
{
    public static final TargetingMixed INSTANCE = new TargetingMixed();

    private TargetingMixed()
    {
        super("Targeting.parts.Mixed");
    }

    /**
     * Processes the recent cross-window mode history.
     */
    public void analyze(final User user,
                        final TargetingContext context,
                        final TargetingMixedAnalysis.Result result)
    {
        final var counter = user.getData().counter.targetingMixedFails;
        if (!result.suspicious()) {
            counter.decrementAboveZero();
            return;
        }

        if (!counter.incrementCompareThreshold()) return;
        getManagement().flag(Flag.of(user)
                                 .setAddedVl(8)
                                 .setDebug(() -> debugMessage(user, context, result)));
    }

    private static String debugMessage(final User user,
                                       final TargetingContext context,
                                       final TargetingMixedAnalysis.Result result)
    {
        return String.format(Locale.ROOT,
                             "TargetingData-Debug | Player: %s repeatedly changed suspicious targeting strategy " +
                             "(context: %s, suspicious_windows: %d, distinct_modes: %d, transitions: %d, mask: %d)",
                             user.getPlayer().getName(),
                             context,
                             result.suspiciousObservations(),
                             result.distinctModes(),
                             result.transitions(),
                             result.observedModeMask());
    }

    @Override
    public ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(500, 2).build();
    }
}
