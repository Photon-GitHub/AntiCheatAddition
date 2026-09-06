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
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

public final class PacketAnalysisStab extends ViolationModule
{
    public static final PacketAnalysisStab INSTANCE = new PacketAnalysisStab();

    private PacketAnalysisStab()
    {
        super("PacketAnalysis.parts.Stab");
    }

    private void onDigging(final PacketReceiveEvent event, final User user)
    {
        if (event.isCancelled()) return;
        final WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
        if (packet.getAction() != DiggingAction.STAB) return;

        final var itemMeta = user.getPlayer().getInventory().getItemInMainHand().getItemMeta();
        if (itemMeta == null || !itemMeta.hasPiercingWeapon()) {
            getManagement().flag(Flag.of(user)
                                     .setAddedVl(50)
                                     .setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " sent a STAB action without a piercing weapon in the main hand."));
        }
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(200, 2).build();
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .setAllowedServerVersions(ServerVersion.MC121_11.getSupVersionsFrom())
                           .addPacketListeners(PacketAdapterBuilder.of(this, PacketType.Play.Client.PLAYER_DIGGING)
                                                                   .priority(PacketListenerPriority.LOW)
                                                                   .onReceiving(this::onDigging).build())
                           .build();
    }
}
