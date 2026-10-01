package de.photon.anticheataddition.modules.checks.packetanalysis;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.subdata.TickPacketData;
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

import java.util.function.Function;

/**
 * Vanilla emits at most one input update and one boat paddle update per client tick.
 * This check enforces this behavior.
 */
public final class PacketAnalysisTickBounded extends ViolationModule
{
    public static final PacketAnalysisTickBounded PLAYER_INPUT = new PacketAnalysisTickBounded("PlayerInputPacketsPerTick", PacketType.Play.Client.PLAYER_INPUT, user -> user.getData().object.packetAnalysisInputPackets);
    public static final PacketAnalysisTickBounded BOAT_PADDLES = new PacketAnalysisTickBounded("BoatPaddlePacketsPerTick", PacketType.Play.Client.STEER_BOAT, user -> user.getData().object.packetAnalysisBoatPaddlePackets);

    private final PacketType.Play.Client packetType;
    private final Function<User, TickPacketData> state;

    private PacketAnalysisTickBounded(String name, PacketType.Play.Client packetType, Function<User, TickPacketData> state)
    {
        super("PacketAnalysis.parts." + name);
        this.packetType = packetType;
        this.state = state;
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .setAllowedServerVersions(ServerVersion.MC121_5.getSupVersionsFrom())
                           .addPacketListeners(PacketAdapterBuilder
                                                       .of(this, packetType, PacketType.Play.Client.CLIENT_TICK_END)
                                                       .priority(PacketListenerPriority.MONITOR)
                                                       .onReceivingRaw(this::onPacketReceive)
                                                       .build())
                           .build();
    }

    private void onPacketReceive(PacketReceiveEvent event)
    {
        final User user = User.getUser(event);
        if (user == null) return;
        final TickPacketData data = state.apply(user);
        if (User.isUserInvalid(user, this)) {
            data.reset();
            return;
        }
        // Cancellation changes server processing, not what the client emitted in this tick.
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_TICK_END) data.endTick();
        else if (data.recordPacket(System.nanoTime()))
            getManagement().flag(Flag.of(user).setAddedVl(20).setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " sent more than one " + packetType + " packet in a client tick."));
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).emptyThresholdManagement().withDecay(200, 2).build();
    }
}
