package de.photon.anticheataddition.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.protocol.packetwrappers.sentbyclient.IWrapperPlayClientLook;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import lombok.val;

public final class PacketAnalysisIllegalPitch extends ViolationModule
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

    @Override
    protected ModuleLoader createModuleLoader()
    {
        val packetAdapter = PacketAdapterBuilder
                .of(this, PacketType.Play.Client.LOOK, PacketType.Play.Client.POSITION_LOOK)
                .priority(ListenerPriority.LOW)
                .onReceiving((event, user) -> {
                    final IWrapperPlayClientLook lookWrapper = event::getPacket;
                    if (!MathUtil.inRange(-90, 90, lookWrapper.getPitch())) {
                        getManagement().flag(Flag.of(user).setAddedVl(150).setDebug("PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " sent illegal pitch value."));
                    }
                }).build();

        return ModuleLoader.builder(this)
                           .addPacketListeners(packetAdapter)
                           .build();
    }
}
