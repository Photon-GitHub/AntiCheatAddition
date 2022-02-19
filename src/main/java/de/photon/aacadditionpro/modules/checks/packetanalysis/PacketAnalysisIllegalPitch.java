package de.photon.aacadditionpro.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.protocol.PacketAdapterBuilder;
import de.photon.aacadditionpro.protocol.packetwrappers.sentbyclient.IWrapperPlayClientLook;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.mathematics.MathUtil;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.val;

public class PacketAnalysisIllegalPitch extends ViolationModule
{
    public PacketAnalysisIllegalPitch()
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
                .of(PacketType.Play.Client.LOOK, PacketType.Play.Client.POSITION_LOOK)
                .priority(ListenerPriority.LOW)
                .onReceiving(event -> {
                    val user = User.safeGetUserFromPacketEvent(event);
                    if (User.isUserInvalid(user, this)) return;

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
