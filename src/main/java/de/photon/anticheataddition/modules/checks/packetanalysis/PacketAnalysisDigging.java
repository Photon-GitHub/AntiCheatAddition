package de.photon.anticheataddition.modules.checks.packetanalysis;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.subdata.PlayerActionData;
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

public final class PacketAnalysisDigging extends ViolationModule
{
    public static final PacketAnalysisDigging INSTANCE = new PacketAnalysisDigging();

    private PacketAnalysisDigging()
    {
        super("PacketAnalysis.parts.Digging");
    }

    private void onDigging(final PacketReceiveEvent event, final User user)
    {
        if (event.isCancelled()) return;
        final WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
        final PlayerActionData state = user.getData().object.playerActionData;

        switch (packet.getAction()) {
            case START_DIGGING -> {
                if (invalidTarget(packet)) {
                    invalid(user, "invalid digging target");
                    return;
                }
                state.startDigging(packet.getBlockPosition().x, packet.getBlockPosition().y, packet.getBlockPosition().z, packet.getBlockFaceId());
            }
            case CANCELLED_DIGGING -> {
                // A client can cancel a stale target after it has already retargeted while the
                // break button remains held. Cancellation is a synchronization hint, not a
                // reliable indication that the packet's target is the currently tracked target.
                state.cancelDigging();
            }
            case FINISHED_DIGGING -> {
                if (invalidTarget(packet)) {
                    invalid(user, "invalid digging target");
                    state.clearDigging();
                    return;
                }

                final PlayerActionData.TransitionResult result = state.finishDigging(packet.getBlockPosition().x, packet.getBlockPosition().y, packet.getBlockPosition().z, packet.getBlockFaceId());
                if (result == PlayerActionData.TransitionResult.INVALID) {
                    invalid(user, "finished a different block than the active dig target");
                }
            }
            default -> {
                // Other action kinds have their own focused submodules.
            }
        }
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.of(this, PacketAdapterBuilder.of(this, PacketType.Play.Client.PLAYER_DIGGING)
                                                         .priority(PacketListenerPriority.LOW)
                                                         .onReceiving(this::onDigging).build());
    }

    private static boolean invalidTarget(final WrapperPlayClientPlayerDigging packet)
    {
        return packet.getBlockPosition() == null || packet.getBlockFaceId() < BlockFace.DOWN.getFaceValue() ||
               packet.getBlockFaceId() > BlockFace.EAST.getFaceValue();
    }

    private void invalid(final User user, final String reason)
    {
        getManagement().flag(Flag.of(user).setAddedVl(20).setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " sent an invalid digging action (" + reason + ")."));
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).emptyThresholdManagement().withDecay(200, 3).build();
    }
}
