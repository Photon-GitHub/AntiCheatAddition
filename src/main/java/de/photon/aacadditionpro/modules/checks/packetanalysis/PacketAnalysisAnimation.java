package de.photon.aacadditionpro.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ModulePacketAdapter;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.DataKey;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import de.photon.aacadditionpro.util.packetwrappers.sentbyclient.WrapperPlayClientUseEntity;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.val;

public class PacketAnalysisAnimation extends ViolationModule
{
    public PacketAnalysisAnimation()
    {
        super("PacketAnalysis.parts.Animation");
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
        val packetAdapter = new AnimationPacketAdapter(this);
        return ModuleLoader.builder(this)
                           .addAllowedServerVersions(ServerVersion.NON_188_VERSIONS)
                           .addPacketListeners(packetAdapter)
                           .build();
    }

    private class AnimationPacketAdapter extends ModulePacketAdapter
    {
        public AnimationPacketAdapter(Module module)
        {
            super(module, ListenerPriority.LOW,
                  // THIS IS IN THE ORDER OF HOW THE PACKETS ARE SUPPOSED TO ARRIVE.
                  PacketType.Play.Client.USE_ENTITY,
                  PacketType.Play.Client.ARM_ANIMATION);
        }

        @Override
        public void onPacketReceiving(final PacketEvent packetEvent)
        {
            val user = User.safeGetUserFromPacketEvent(packetEvent);
            if (User.isUserInvalid(user, this.getModule())) return;

            val packetType = packetEvent.getPacketType();

            if (packetType == PacketType.Play.Client.ARM_ANIMATION) user.getDataMap().setBoolean(DataKey.BooleanKey.PACKET_ANALYSIS_ANIMATION_EXPECTED, false);
            else if (packetType == PacketType.Play.Client.USE_ENTITY) {
                // Expected Animation after attack, but didn't arrive.
                if (user.getDataMap().getBoolean(DataKey.BooleanKey.PACKET_ANALYSIS_ANIMATION_EXPECTED)) {
                    user.getDataMap().setBoolean(DataKey.BooleanKey.PACKET_ANALYSIS_ANIMATION_EXPECTED, false);
                    getManagement().flag(Flag.of(user).setAddedVl(10).setEventNotCancelledAction(() -> DebugSender.getInstance().sendDebug("PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " did not send animation packet after an attack.")));
                }

                // Make sure an arm animation packet is sent directly after an attack as it is the next packet in the client code.
                val useEntityWrapper = new WrapperPlayClientUseEntity(packetEvent.getPacket());
                if (useEntityWrapper.getType() == EnumWrappers.EntityUseAction.ATTACK) user.getDataMap().setBoolean(DataKey.BooleanKey.PACKET_ANALYSIS_ANIMATION_EXPECTED, true);
            }
        }
    }
}
