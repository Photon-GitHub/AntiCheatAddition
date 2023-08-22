package de.photon.anticheataddition.modules.checks.packetanalysis;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PacketAnalysisPerfectRotation extends ViolationModule implements Listener
{
    public static final PacketAnalysisPerfectRotation INSTANCE = new PacketAnalysisPerfectRotation();

    private PacketAnalysisPerfectRotation()
    {
        super("PacketAnalysis.parts.PerfectRotation");
    }

    private static final double EQUALITY_EPSILON = 0.00001;
    private static final double[] MULTIPLE_PATTERNS = {0.1, 0.25};

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event)
    {
        final var user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this) || event.getTo() == null) return;

        final double[] diffs = {MathUtil.absDiff(event.getTo().getYaw(), event.getFrom().getYaw()),
                                MathUtil.absDiff(event.getTo().getPitch(), event.getFrom().getPitch())};

        for (double d : diffs) {
            if (MathUtil.absDiff(d, 0) <= EQUALITY_EPSILON) continue;

            if (Double.isInfinite(d) || Double.isNaN(d))
                getManagement().flag(Flag.of(user).setAddedVl(10).setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " sent infinite rotation diffs."));

            // Check if the angle change is a multiple of any pattern like 0.1 or 0.25.
            for (double pattern : MULTIPLE_PATTERNS) {
                final double potentialMultiple = d / pattern;

                if (MathUtil.absDiff(potentialMultiple, Math.rint(potentialMultiple)) <= EQUALITY_EPSILON)
                    getManagement().flag(Flag.of(user).setAddedVl(10).setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " sent suspicious rotation diffs (multiple of " + pattern + ")."));
            }
        }
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(200, 1).build();
    }
}
