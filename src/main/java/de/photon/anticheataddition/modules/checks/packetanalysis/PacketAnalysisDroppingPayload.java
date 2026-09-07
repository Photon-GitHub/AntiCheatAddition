package de.photon.anticheataddition.modules.checks.packetanalysis;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

/**
 * Checks unused target fields on drop, release and swap actions, independently of use/dig state.
 */
public final class PacketAnalysisDroppingPayload extends ViolationModule
{
    public static final PacketAnalysisDroppingPayload INSTANCE = new PacketAnalysisDroppingPayload();

    private PacketAnalysisDroppingPayload()
    {
        super("PacketAnalysis.parts.DroppingPayload");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.of(this, PacketAdapterBuilder.of(this, PacketType.Play.Client.PLAYER_DIGGING)
                                                         .priority(PacketListenerPriority.LOW)
                                                         .onReceiving((event, user) -> {
                                                             if (event.isCancelled()) return;
                                                             final var packet = new WrapperPlayClientPlayerDigging(event);

                                                             if (!invalidDroppingPayload(packet.getAction(), packet.getBlockPosition(), packet.getBlockFaceId(), event.getUser().getClientVersion())) return;
                                                             getManagement().flag(Flag.of(user).setAddedVl(20).setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " sent an invalid non-block digging payload (action: " + packet.getAction() + ", position: " + packet.getBlockPosition() + ", face: " + packet.getBlockFaceId() + ")."));
                                                         }).build());
    }

    public static boolean invalidDroppingPayload(final DiggingAction action, final Vector3i position, final int face, final ClientVersion clientVersion)
    {
        return switch (action) {
            case DROP_ITEM, DROP_ITEM_STACK, RELEASE_USE_ITEM, SWAP_ITEM_WITH_OFFHAND -> {
                if (clientVersion == null || clientVersion == ClientVersion.UNKNOWN) yield false;
                // Missing decoded fields are not evidence for this rule about target values.
                if (position == null) yield false;

                yield position.x != 0 || position.y != 0 || position.z != 0 || face != 0;
            }

            // Ignore block-based actions and stab.
            case null, default -> false;
        };
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).emptyThresholdManagement().withDecay(200, 2).build();
    }
}
