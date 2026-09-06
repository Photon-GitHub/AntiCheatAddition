package de.photon.anticheataddition.modules.checks.packetanalysis;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.data.subdata.PlayerActionData;
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

public final class PacketAnalysisSequence extends ViolationModule
{
    public static final PacketAnalysisSequence INSTANCE = new PacketAnalysisSequence();

    private PacketAnalysisSequence()
    {
        super("PacketAnalysis.parts.Sequence");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .setAllowedServerVersions(ServerVersion.MC119.getSupVersionsFrom())
                           .addPacketListeners(PacketAdapterBuilder
                                                       .of(this, PacketType.Play.Client.PLAYER_DIGGING)
                                                       .priority(PacketListenerPriority.LOW)
                                                       .onReceiving((event, user) -> {
                                                           if (event.isCancelled()) return;

                                                           final WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
                                                           final PlayerActionData state = user.getData().object.playerActionData;

                                                           // CANCELLED_DIGGING intentionally uses sequence zero in vanilla clients.
                                                           if (packet.getAction() != DiggingAction.START_DIGGING && packet.getAction() != DiggingAction.FINISHED_DIGGING) return;

                                                           final ClientVersion clientVersion = event.getUser().getClientVersion();
                                                           final boolean sequenceSupported = clientVersion != null && clientVersion.isNewerThanOrEquals(ClientVersion.V_1_19);
                                                           if (state.observeSequence(packet.getSequence(), sequenceSupported)) return;

                                                           final int addedVl = packet.getSequence() < 0 ? 150 : 10;
                                                           getManagement().flag(Flag.of(user)
                                                                                    .setAddedVl(addedVl)
                                                                                    .setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " sent an invalid PLAYER_DIGGING sequence (" + packet.getSequence() + ")."));
                                                       }).build())
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
