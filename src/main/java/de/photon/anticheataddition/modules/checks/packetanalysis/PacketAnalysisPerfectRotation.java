package de.photon.anticheataddition.modules.checks.packetanalysis;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.ViolationCounter;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Checks for suspicious packet rotation patterns that correspond to precise values like 0.25 or 0.1.
 */
public final class PacketAnalysisPerfectRotation extends ViolationModule implements Listener
{
    public static final PacketAnalysisPerfectRotation INSTANCE = new PacketAnalysisPerfectRotation();

    private PacketAnalysisPerfectRotation()
    {
        super("PacketAnalysis.parts.PerfectRotation");
    }

    private static final double EQUALITY_EPSILON = 0.0000000001;
    private static final double[] MULTIPLE_PATTERNS = {0.1, 0.25};

    private static boolean isNearlyEqual(double reference, double d)
    {
        return MathUtil.absDiff(reference, d) <= EQUALITY_EPSILON;
    }

    /**
     * Checks if the second value is an integer multiple of the first.
     *
     * @param reference The reference value.
     * @param d         The value to check.
     *
     * @return True if d is an integer multiple of reference, false otherwise.
     */
    private static boolean isIntegerMultiple(double reference, double d)
    {
        final double potentialMultiple = d / reference;
        return isNearlyEqual(potentialMultiple, Math.rint(potentialMultiple));
    }

    /**
     * Checks if the provided rotation value represents no change in rotation.
     * No rotation is defined as any rotation by 0 degrees or a multiple of 360 degrees.
     *
     * @param rotation The rotation value.
     *
     * @return True if there's no rotation, false otherwise.
     */
    private static boolean noRotation(double rotation)
    {
        // 0 degrees or full circles (multiple of 360 degrees)
        return isNearlyEqual(0, rotation) || isIntegerMultiple(360, rotation);
    }

    /**
     * Checks the pattern of rotation changes and flags suspicious rotations.
     *
     * @param user         The user under check.
     * @param rotationDiff The rotation difference.
     * @param counter      The violation counter.
     */
    private void checkPatterns(User user, double rotationDiff, ViolationCounter counter)
    {
        // Ignore 0 and 360 degrees as they represent no change in rotation.
        if (noRotation(rotationDiff)) return;

        // Check if the angle change is valid (not infinite or NaN).
        if (Double.isInfinite(rotationDiff) || Double.isNaN(rotationDiff))
            getManagement().flag(Flag.of(user).setAddedVl(5).setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " sent infinite rotation diffs."));

        for (double pattern : MULTIPLE_PATTERNS) {
            // Normal players sometimes have a multiple, but not consistently -> counter.
            if (counter.conditionallyIncDec(isIntegerMultiple(pattern, rotationDiff))) {
                getManagement().flag(Flag.of(user).setAddedVl(3).setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " sent suspicious rotation diff (" + rotationDiff + ")."));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event)
    {
        final var user = User.getUser(event.getPlayer());
        // Ignore invalid users and players in vehicles (boat false positive).
        if (User.isUserInvalid(user, this) || event.getTo() == null || user.getPlayer().isInsideVehicle()) return;

        final double yawDiff = MathUtil.yawDistance(event.getTo().getYaw(), event.getFrom().getYaw());
        final double pitchDiff = MathUtil.absDiff(event.getTo().getPitch(), event.getFrom().getPitch());

        checkPatterns(user, yawDiff, user.getData().counter.packetAnalysisPerfectRotationYawFails);
        checkPatterns(user, pitchDiff, user.getData().counter.packetAnalysisPerfectRotationPitchFails);
    }


    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(100, 2).build();
    }
}
