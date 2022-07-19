package de.photon.anticheataddition.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.wrappers.EnumWrappers;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.protocol.packetwrappers.sentbyclient.WrapperPlayClientUseEntity;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import lombok.val;

public final class PacketAnalysisAnimation extends ViolationModule
{
    public static final PacketAnalysisAnimation INSTANCE = new PacketAnalysisAnimation();

    private PacketAnalysisAnimation()
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
        /* Protocol:
         * 1) Player left clicks
         * 2) Entity use packet with attack.
         * 3) Arm Animation packet.
         * */
        val packetAdapter = PacketAdapterBuilder
                .of(this, PacketType.Play.Client.USE_ENTITY, PacketType.Play.Client.ARM_ANIMATION)
                .priority(ListenerPriority.LOW)
                .onReceiving((event, user) -> {
                    val packetType = event.getPacketType();

                    if (packetType == PacketType.Play.Client.ARM_ANIMATION) user.getData().bool.setPacketAnalysisAnimationExpected(false);
                    else if (packetType == PacketType.Play.Client.USE_ENTITY) {
                        // Expected Animation after attack, but didn't arrive.
                        if (user.getData().bool.isPacketAnalysisAnimationExpected()) {
                            user.getData().bool.setPacketAnalysisAnimationExpected(false);
                            getManagement().flag(Flag.of(user).setAddedVl(30).setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " did not send animation packet after an attack."));
                        }

                        // Make sure an arm animation packet is sent directly after an attack as it is the next packet in the client code.
                        val useEntityWrapper = new WrapperPlayClientUseEntity(event.getPacket());
                        if (useEntityWrapper.getType() == EnumWrappers.EntityUseAction.ATTACK)
                            user.getData().bool.setPacketAnalysisAnimationExpected(true);
                    }
                }).build();

        return ModuleLoader.builder(this)
                           .setAllowedServerVersions(ServerVersion.NON_188_VERSIONS)
                           .addPacketListeners(packetAdapter)
                           .build();
    }
}
