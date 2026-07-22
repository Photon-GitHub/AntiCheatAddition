package de.photon.anticheataddition.modules.checks.targeting;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.subdata.TargetingAcquisitionData;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

import java.util.Locale;

/**
 * Detects unusually repeatable target-bound deceleration across successful player acquisitions.
 *
 * <p>Slowing down near a target is normal and never flags by itself. Evidence is added only after many independent,
 * comparable acquisitions repeatedly use nearly the same slowdown ratio, activation distance, and normalized speed
 * curve. The event-side filters further restrict analysis to stable ground-based player-versus-player situations.</p>
 */
public final class TargetingAcquisition extends ViolationModule
{
    public static final TargetingAcquisition INSTANCE = new TargetingAcquisition();

    private static final int ADDED_VL = 15;
    private static final int SEVERE_ADDED_VL = 25;

    private TargetingAcquisition()
    {
        super("Targeting.parts.Acquisition");
    }

    /**
     * Stores one valid profile and evaluates the player's longer-term acquisition consistency.
     */
    public void analyze(final User user, final TargetingAcquisitionAnalysis.Profile profile)
    {
        final TargetingAcquisitionData.Assessment assessment = user.getTargetingAcquisitionData().add(profile);
        final var counter = user.getData().counter.targetingAcquisitionFails;
        if (!assessment.enoughData()) return;

        if (!assessment.suspicious()) {
            counter.decrementAboveZero();
            return;
        }

        final int evidence = assessment.severe() ? 2 : 1;
        if (!counter.incrementCompareThreshold(evidence)) return;
        getManagement().flag(Flag.of(user)
                                 .setAddedVl(assessment.severe() ? SEVERE_ADDED_VL : ADDED_VL)
                                 .setDebug(() -> debugMessage(user, profile, assessment)));
    }

    private static String debugMessage(final User user,
                                       final TargetingAcquisitionAnalysis.Profile profile,
                                       final TargetingAcquisitionData.Assessment assessment)
    {
        return String.format(Locale.ROOT,
                             "TargetingData-Debug | Player: %s repeatedly slowed rotations at a target with an " +
                             "unusually stable acquisition curve (profiles: %d, candidates: %d, signals: %d, " +
                             "candidate_ratio: %.3f, mean_slowdown: %.3f, slowdown_sd: %.4f, activation_cv: %.3f, " +
                             "profile_corr: %.3f, profile_rmse: %.4f, approach_efficiency: %.3f, toward_ratio: %.3f, " +
                             "latest_initial_error: %.3f, latest_final_error: %.3f, latest_activation: %.3f, " +
                             "latest_distance: %.3f)",
                             user.getPlayer().getName(),
                             assessment.comparableProfiles(),
                             assessment.candidateProfiles(),
                             assessment.independentSignals(),
                             assessment.candidateRatio(),
                             assessment.meanSlowdownRatio(),
                             assessment.slowdownStandardDeviation(),
                             assessment.activationCoefficientOfVariation(),
                             assessment.meanProfileCorrelation(),
                             assessment.profileRootMeanSquareError(),
                             assessment.meanApproachEfficiency(),
                             assessment.meanTowardRatio(),
                             profile.initialError(),
                             profile.finalError(),
                             profile.activationError(),
                             profile.targetDistance());
    }

    @Override
    public ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(600, 2).build();
    }
}
