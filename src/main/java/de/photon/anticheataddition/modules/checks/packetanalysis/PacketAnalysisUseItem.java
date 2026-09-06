package de.photon.anticheataddition.modules.checks.packetanalysis;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.subdata.PlayerActionData;
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

public final class PacketAnalysisUseItem extends ViolationModule
{
    public static final PacketAnalysisUseItem INSTANCE = new PacketAnalysisUseItem();

    private PacketAnalysisUseItem()
    {
        super("PacketAnalysis.parts.UseItem");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .setAllowedServerVersions(ServerVersion.NON_188_VERSIONS)
                           .addPacketListeners(PacketAdapterBuilder.of(this, PacketType.Play.Client.PLAYER_DIGGING)
                                                                   .priority(PacketListenerPriority.LOW)
                                                                   .onReceiving(this::onDigging).build())

                           .addPacketListeners(PacketAdapterBuilder.of(this, PacketType.Play.Client.USE_ITEM)
                                                                   .priority(PacketListenerPriority.LOW)
                                                                   .onReceiving((event, user) -> user.getData().object.playerActionData.startUse()).build())
                           .build();
    }

    private void onDigging(final PacketReceiveEvent event, final User user)
    {
        if (event.isCancelled()) return;
        final WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
        final PlayerActionData state = user.getData().object.playerActionData;

        if (packet.getAction() == DiggingAction.SWAP_ITEM_WITH_OFFHAND) {
            // Vanilla stops active item use when the hands are swapped. Keep this transition with the use-item state
            // machine so its lifecycle stays local to this check.
            state.clearUse();
            return;
        }
        if (packet.getAction() != DiggingAction.RELEASE_USE_ITEM) return;

        if (state.releaseUse() == PlayerActionData.TransitionResult.INVALID) {
            getManagement().flag(Flag.of(user)
                                     .setAddedVl(10)
                                     .setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " repeated RELEASE_USE_ITEM without an active use."));
        }
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(200, 2).build();
    }
}
