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

public final class PacketAnalysisIllegalPitch extends ViolationModule implements Listener
{
    public static final PacketAnalysisIllegalPitch INSTANCE = new PacketAnalysisIllegalPitch();

    private PacketAnalysisIllegalPitch()
    {
        super("PacketAnalysis.parts.IllegalPitch");
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(200, 1).build();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event)
    {
        final var user = User.getUser(event.getPlayer().getUniqueId());
        if (User.isUserInvalid(user, this) || event.getTo() == null) return;

        if (!MathUtil.inRange(-90, 90, event.getTo().getPitch())) {
            getManagement().flag(Flag.of(user).setAddedVl(150).setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " sent illegal pitch value."));
        }
    }
}
