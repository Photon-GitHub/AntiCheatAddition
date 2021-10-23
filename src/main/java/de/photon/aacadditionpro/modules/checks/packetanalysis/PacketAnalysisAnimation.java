package de.photon.aacadditionpro.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.wrappers.EnumWrappers;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.protocol.PacketAdapterBuilder;
import de.photon.aacadditionpro.protocol.packetwrappers.sentbyclient.WrapperPlayClientUseEntity;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.DataKey;
import de.photon.aacadditionpro.util.messaging.DebugSender;
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
                                       .withDecay(200, 2).build();
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        val packetAdapter = PacketAdapterBuilder
                // THIS IS IN THE ORDER OF HOW THE PACKETS ARE SUPPOSED TO ARRIVE.
                .of(PacketType.Play.Client.USE_ENTITY,
                    PacketType.Play.Client.ARM_ANIMATION)
                .priority(ListenerPriority.LOW)
                .onReceiving(event -> {
                    val user = User.safeGetUserFromPacketEvent(event);
                    if (User.isUserInvalid(user, this)) return;

                    val packetType = event.getPacketType();

                    if (packetType == PacketType.Play.Client.ARM_ANIMATION)
                        user.getDataMap().setBoolean(DataKey.BooleanKey.PACKET_ANALYSIS_ANIMATION_EXPECTED, false);
                    else if (packetType == PacketType.Play.Client.USE_ENTITY) {
                        // Expected Animation after attack, but didn't arrive.
                        if (user.getDataMap().getBoolean(DataKey.BooleanKey.PACKET_ANALYSIS_ANIMATION_EXPECTED)) {
                            user.getDataMap().setBoolean(DataKey.BooleanKey.PACKET_ANALYSIS_ANIMATION_EXPECTED, false);
                            getManagement().flag(Flag.of(user).setAddedVl(30).setEventNotCancelledAction(() -> DebugSender.getInstance().sendDebug("PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " did not send animation packet after an attack.")));
                        }

                        // Make sure an arm animation packet is sent directly after an attack as it is the next packet in the client code.
                        val useEntityWrapper = new WrapperPlayClientUseEntity(event.getPacket());
                        if (useEntityWrapper.getType() == EnumWrappers.EntityUseAction.ATTACK)
                            user.getDataMap().setBoolean(DataKey.BooleanKey.PACKET_ANALYSIS_ANIMATION_EXPECTED, true);
                    }
                }).build();

        return ModuleLoader.builder(this)
                           .addAllowedServerVersions(ServerVersion.NON_188_VERSIONS)
                           .addPacketListeners(packetAdapter)
                           .build();
    }
}
