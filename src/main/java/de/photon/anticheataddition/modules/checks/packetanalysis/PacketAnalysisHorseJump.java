package de.photon.anticheataddition.modules.checks.packetanalysis;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;

/**
 * Validates the action's auxiliary value without assuming a synchronized mount or sprint state.
 */
public final class PacketAnalysisHorseJump extends ViolationModule
{
    public static final PacketAnalysisHorseJump INSTANCE = new PacketAnalysisHorseJump();

    private PacketAnalysisHorseJump()
    {
        super("PacketAnalysis.parts.HorseJump");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.of(this, PacketAdapterBuilder.of(this, PacketType.Play.Client.ENTITY_ACTION)
                                                         .priority(PacketListenerPriority.LOW)
                                                         .onReceiving((event, user) -> {
                                                             if (event.isCancelled()) return;
                                                             final var packet = new WrapperPlayClientEntityAction(event);

                                                             if (!invalidActionParameter(packet.getAction(), packet.getJumpBoost())) return;
                                                             getManagement().flag(Flag.of(user).setAddedVl(50).setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " sent an invalid action parameter (action: " + packet.getAction() + ", boost: " + packet.getJumpBoost() + ")."));
                                                         }).build());
    }

    public static boolean invalidActionParameter(final WrapperPlayClientEntityAction.Action action, final int boost)
    {
        // Decoding problem?
        if (action == null) return false;

        // Contrary to the wikivg's 0..100 range, vanilla can send negative horse jump values in the range -100..100.
        if (action == WrapperPlayClientEntityAction.Action.START_JUMPING_WITH_HORSE) return boost < -100 || boost > 100;

        // Boost for non-boost actions.
        return boost != 0;
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).emptyThresholdManagement().withDecay(200, 2).build();
    }
}
