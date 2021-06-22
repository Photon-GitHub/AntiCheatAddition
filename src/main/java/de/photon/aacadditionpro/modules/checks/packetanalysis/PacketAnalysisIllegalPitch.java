package de.photon.aacadditionpro.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ModulePacketAdapter;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.mathematics.MathUtil;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import de.photon.aacadditionpro.util.packetwrappers.sentbyclient.IWrapperPlayClientLook;
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
        val packetAdapter = new IllegalPitchAdapter(this);
        return ModuleLoader.builder(this)
                           .addPacketListeners(packetAdapter)
                           .build();
    }

    private class IllegalPitchAdapter extends ModulePacketAdapter
    {
        public IllegalPitchAdapter(Module module)
        {
            super(module, ListenerPriority.LOW, PacketType.Play.Client.LOOK, PacketType.Play.Client.POSITION_LOOK);
        }

        @Override
        public void onPacketReceiving(final PacketEvent packetEvent)
        {
            val user = User.safeGetUserFromPacketEvent(packetEvent);
            if (User.isUserInvalid(user, this.getModule())) return;

            final IWrapperPlayClientLook lookWrapper = packetEvent::getPacket;
            if (!MathUtil.inRange(-90, 90, lookWrapper.getPitch())) {
                getManagement().flag(Flag.of(user).setAddedVl(20).setEventNotCancelledAction(() -> DebugSender.getInstance().sendDebug("PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " sent illegal pitch value.")));
            }
        }
    }
}
