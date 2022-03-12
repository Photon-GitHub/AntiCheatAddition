package de.photon.anticheataddition.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.wrappers.EnumWrappers;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.protocol.packetwrappers.sentbyclient.WrapperPlayClientUseEntity;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.DataKey;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
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
                        user.getDataMap().setBoolean(DataKey.Bool.PACKET_ANALYSIS_ANIMATION_EXPECTED, false);
                    else if (packetType == PacketType.Play.Client.USE_ENTITY) {
                        // Expected Animation after attack, but didn't arrive.
                        if (user.getDataMap().getBoolean(DataKey.Bool.PACKET_ANALYSIS_ANIMATION_EXPECTED)) {
                            user.getDataMap().setBoolean(DataKey.Bool.PACKET_ANALYSIS_ANIMATION_EXPECTED, false);
                            getManagement().flag(Flag.of(user).setAddedVl(30).setDebug("PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " did not send animation packet after an attack."));
                        }

                        // Make sure an arm animation packet is sent directly after an attack as it is the next packet in the client code.
                        val useEntityWrapper = new WrapperPlayClientUseEntity(event.getPacket());
                        if (useEntityWrapper.getType() == EnumWrappers.EntityUseAction.ATTACK)
                            user.getDataMap().setBoolean(DataKey.Bool.PACKET_ANALYSIS_ANIMATION_EXPECTED, true);
                    }
                }).build();

        return ModuleLoader.builder(this)
                           .setAllowedServerVersions(ServerVersion.NON_188_VERSIONS)
                           .addPacketListeners(packetAdapter)
                           .build();
    }
}
