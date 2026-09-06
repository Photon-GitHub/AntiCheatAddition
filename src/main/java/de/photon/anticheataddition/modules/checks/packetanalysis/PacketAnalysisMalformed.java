package de.photon.anticheataddition.modules.checks.packetanalysis;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

public final class PacketAnalysisMalformed extends ViolationModule
{
    public static final PacketAnalysisMalformed INSTANCE = new PacketAnalysisMalformed();

    private PacketAnalysisMalformed()
    {
        super("PacketAnalysis.parts.Malformed");
    }

    private void onDigging(final PacketReceiveEvent event, final User user)
    {
        try {
            final var packet = new WrapperPlayClientPlayerDigging(event);
            if (packet.getAction() == null) throw new IllegalArgumentException("Missing digging action");
        } catch (RuntimeException exception) {
            event.setCancelled(true);
            getManagement().flag(Flag.of(user)
                                     .setAddedVl(150)
                                     .setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " sent a malformed PLAYER_DIGGING packet (" + exception.getClass().getSimpleName() + ")."));
        }
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .setAllowedServerVersions(ServerVersion.MC119.getSupVersionsFrom())
                           .addPacketListeners(PacketAdapterBuilder.of(this, PacketType.Play.Client.PLAYER_DIGGING)
                                                                   .priority(PacketListenerPriority.LOWEST)
                                                                   .onReceiving(this::onDigging).build())
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(200, 5).build();
    }
}
