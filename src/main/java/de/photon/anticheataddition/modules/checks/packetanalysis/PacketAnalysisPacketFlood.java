package de.photon.anticheataddition.modules.checks.packetanalysis;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

import java.util.concurrent.TimeUnit;

public final class PacketAnalysisPacketFlood extends ViolationModule
{
    public static final PacketAnalysisPacketFlood INSTANCE = new PacketAnalysisPacketFlood();

    private final double sustainedPacketsPerSecond = Math.max(1D, loadDouble(".sustained_packets_per_second", 40D));
    private final double burstCapacity = Math.max(1D, loadDouble(".burst_capacity", 200D));
    private final long sustainedDurationNanos = TimeUnit.MILLISECONDS.toNanos(Math.max(1L, loadLong(".sustained_violation_duration_ms", 3000L)));

    private PacketAnalysisPacketFlood()
    {
        super("PacketAnalysis.parts.PacketFlood");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        final var packetAdapter = PacketAdapterBuilder
                .of(this, PacketType.Play.Client.PLAYER_FLYING, PacketType.Play.Client.PLAYER_POSITION, PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION, PacketType.Play.Client.PLAYER_ROTATION)
                .priority(PacketListenerPriority.LOW)
                .onReceiving((event, user) -> {
                    final long timestamp = System.nanoTime();
                    final var state = user.getData().object.packetFloodData;
                    final var result = state.record(timestamp, sustainedPacketsPerSecond, burstCapacity, sustainedDurationNanos);

                    if (result.sustained() && state.shouldReport(timestamp, sustainedDurationNanos)) {
                        getManagement().flag(Flag.of(user)
                                                 .setAddedVl(5)
                                                 .setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " sustained an excessive movement packet rate."));
                    }
                }).build();
        return ModuleLoader.of(this, packetAdapter);
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).emptyThresholdManagement().withDecay(200, 1).build();
    }
}
