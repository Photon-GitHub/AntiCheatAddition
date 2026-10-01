package de.photon.anticheataddition.modules.checks.packetanalysis;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

/**
 * Checks for more than one player movement packet in a client tick.
 * <p>
 * Vanilla sends at most one position update per tick. Position, rotation, combined position-and-rotation, and
 * ground-only packets are mutually exclusive encodings of that update, so their combined count must stay at one.
 * Minecraft added the serverbound client-tick-end packet in 1.21.2; older clients do not provide this boundary and
 * are excluded rather than counted across multiple ticks.
 */
public final class PacketAnalysisMovementPacketsPerTick extends ViolationModule
{
    public static final PacketAnalysisMovementPacketsPerTick INSTANCE = new PacketAnalysisMovementPacketsPerTick();

    private PacketAnalysisMovementPacketsPerTick()
    {
        super("PacketAnalysis.parts.MovementPacketsPerTick");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           // CLIENT_TICK_END was added in 1.21.2. The first supported server release is 1.21.5.
                           .setAllowedServerVersions(ServerVersion.MC121_5.getSupVersionsFrom())
                           .addPacketListeners(PacketAdapterBuilder
                                                       .of(this, PacketType.Play.Client.PLAYER_FLYING, PacketType.Play.Client.PLAYER_POSITION, PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION, PacketType.Play.Client.PLAYER_ROTATION, PacketType.Play.Client.CLIENT_TICK_END)
                                                       .priority(PacketListenerPriority.LOW)
                                                       .onReceiving((event, user) -> {
                                                           if (event.isCancelled()) return;

                                                           if (event.getPacketType() == PacketType.Play.Client.CLIENT_TICK_END) {
                                                               user.getData().counter.packetAnalysisMovementPacketsThisTick.setToZero();
                                                               return;
                                                           }

                                                           if (user.getData().counter.packetAnalysisMovementPacketsThisTick.incrementCompareThreshold()) {
                                                               if (user.getData().counter.packetAnalysisMovementPacketsThisTickFails.incrementCompareThreshold())
                                                                   getManagement().flag(Flag.of(user)
                                                                                            .setAddedVl(20)
                                                                                            .setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " sent more than one movement packet in a client tick."));
                                                           } else {
                                                               user.getData().counter.packetAnalysisMovementPacketsThisTickFails.decrementAboveZero();
                                                           }
                                                       }).build()).build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).emptyThresholdManagement().withDecay(200, 2).build();
    }
}
