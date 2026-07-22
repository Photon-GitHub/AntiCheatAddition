package de.photon.anticheataddition.modules.checks.packetanalysis;


import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.util.protocol.PacketEventUtils;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

public final class PacketAnalysisExtremeYaw extends ViolationModule {
    public static final PacketAnalysisExtremeYaw INSTANCE = new PacketAnalysisExtremeYaw();

    private PacketAnalysisExtremeYaw()
    {
        super("PacketAnalysis.parts.ExtremeYaw");
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(200, 1).build();
    }

    // Any rotation above 1 million degrees is regarded as impossible to achieve even with extreme mouse sensitivity.
    private static final double THRESHOLD = 1_000_000.0;

    @Override
    protected ModuleLoader createModuleLoader()
    {
        final var packetAdapter = PacketAdapterBuilder
                .of(this, PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION, PacketType.Play.Client.PLAYER_ROTATION)
                .priority(PacketListenerPriority.LOW)
                .onReceiving((event, user) -> {
                    final PacketEventUtils.Rotation rotation = PacketEventUtils.getRotationFromEvent(event);

                    if (rotation.yaw() > THRESHOLD || rotation.yaw() < -THRESHOLD) {
                        getManagement().flag(Flag.of(user).setAddedVl(50).setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " sent extreme yaw value."));
                    }
                }).build();

        return ModuleLoader.builder(this)
                           .addPacketListeners(packetAdapter)
                           .build();
    }
}
