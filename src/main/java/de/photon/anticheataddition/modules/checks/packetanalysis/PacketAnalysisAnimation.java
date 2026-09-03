package de.photon.anticheataddition.modules.checks.packetanalysis;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;
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
        return ViolationLevelManagement.builder(this).emptyThresholdManagement().withDecay(200, 2).build();
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        /* Protocol:
         * 1) Player left clicks
         * 2) Entity use packet with attack.
         * 3) Arm Animation packet.
         * */
        return ModuleLoader.builder(this)
                           .setAllowedServerVersions(ServerVersion.NON_188_VERSIONS)
                           .addPacketListeners(PacketAdapterBuilder.of(this, PacketType.Play.Client.ANIMATION, PacketType.Play.Client.ATTACK, PacketType.Play.Client.INTERACT_ENTITY)
                                                                   .priority(PacketListenerPriority.LOW)
                                                                   .onReceiving((event, user) -> {
                                                                       switch (event.getPacketType()) {
                                                                           // We received an animation -> No animation expected anymore.
                                                                           case PacketType.Play.Client.ANIMATION -> user.getData().bool.packetAnalysisAnimationExpected = false;
                                                                           // Potential attack packets.
                                                                           case PacketType.Play.Client.ATTACK -> handleAttack(user);
                                                                           case PacketType.Play.Client.INTERACT_ENTITY -> {
                                                                               final var wrapper = new WrapperPlayClientInteractEntity(event);
                                                                               if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) handleAttack(user);
                                                                           }
                                                                           // Ignore other packets.
                                                                           default -> {}
                                                                       }
                                                                   }).build()).build();
    }

    /**
     * This method is called when an attack packet is received.
     */
    private void handleAttack(final User user)
    {
        // Expected Animation after attack, but didn't arrive.
        if (user.getData().bool.packetAnalysisAnimationExpected)
            getManagement().flag(Flag.of(user).setAddedVl(30).setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " did not send animation packet after an attack."));

        // Reset as we have just received another attack and now expect another animation.
        user.getData().bool.packetAnalysisAnimationExpected = true;
    }
}
