package de.photon.anticheataddition.modules.checks.packetanalysis;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.protocol.PacketEventUtils;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

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
        final var packetAdapter = PacketAdapterBuilder
                .of(this, PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION, PacketType.Play.Client.PLAYER_ROTATION)
                .priority(PacketListenerPriority.LOW)
                .onReceiving((event, user) -> {
                    final PacketEventUtils.Rotation rotation = PacketEventUtils.getRotationFromEvent(event);

                    if (!MathUtil.inRange(-90, 90, rotation.pitch())) {
                        getManagement().flag(Flag.of(user).setAddedVl(150).setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " sent illegal pitch value."));
                    }
                }).build();

        return ModuleLoader.builder(this)
                           .addPacketListeners(packetAdapter)
                           .build();
    }
}
