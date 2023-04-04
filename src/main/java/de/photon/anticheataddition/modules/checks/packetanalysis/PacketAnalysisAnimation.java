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
        final var packetAdapter = PacketAdapterBuilder
                .of(this, PacketType.Play.Client.USE_ENTITY, PacketType.Play.Client.ARM_ANIMATION)
                .priority(ListenerPriority.LOW)
                .onReceiving((event, user) -> {
                    final var packetType = event.getPacketType();
                    final var boolData = user.getData().bool;

                    if (packetType == PacketType.Play.Client.ARM_ANIMATION) boolData.packetAnalysisAnimationExpected = false;
                    else if (packetType == PacketType.Play.Client.USE_ENTITY) {
                        // Expected Animation after attack, but didn't arrive.
                        if (boolData.packetAnalysisAnimationExpected) {
                            boolData.packetAnalysisAnimationExpected = false;
                            getManagement().flag(Flag.of(user).setAddedVl(30).setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " did not send animation packet after an attack."));
                        }

                        // Make sure an arm animation packet is sent directly after an attack as it is the next packet in the client code.
                        final var useEntityWrapper = new WrapperPlayClientUseEntity(event.getPacket());
                        if (useEntityWrapper.getType() == EnumWrappers.EntityUseAction.ATTACK) boolData.packetAnalysisAnimationExpected = true;
                    }
                }).build();

        return ModuleLoader.builder(this)
                           .setAllowedServerVersions(ServerVersion.NON_188_VERSIONS)
                           .addPacketListeners(packetAdapter)
                           .build();
    }
}
