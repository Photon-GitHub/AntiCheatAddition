package de.photon.AACAdditionPro.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PacketListenerModule;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.modules.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.user.data.PacketAnalysisData;
import de.photon.AACAdditionPro.util.packetwrappers.server.WrapperPlayServerPosition;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;

import java.util.Set;

public class PacketAnalysis extends PacketAdapter implements PacketListenerModule, PatternModule, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 200);

    private final AnimationPattern animationPattern = new AnimationPattern();

    private final ComparePattern comparePattern = new ComparePattern();
    private final EqualRotationPattern equalRotationPattern = new EqualRotationPattern();

    private final IllegalPitchPattern illegalPitchPattern = new IllegalPitchPattern();

    private final PositionSpoofPattern positionSpoofPattern = new PositionSpoofPattern();

    public PacketAnalysis()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.LOW,
              // --------------- Server --------------- //
              // Compare
              PacketType.Play.Server.POSITION,
              // --------------- Client --------------- //
              // CombatOrder
              PacketType.Play.Client.USE_ENTITY,
              PacketType.Play.Client.ARM_ANIMATION,
              // EqualRotation
              PacketType.Play.Client.LOOK,
              // EqualRotation + Compare
              PacketType.Play.Client.POSITION_LOOK);
    }

    @Override
    public void onPacketSending(PacketEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.POSITION) {
            user.getPacketAnalysisData().lastPositionForceData = new PacketAnalysisData.PositionForceData(new WrapperPlayServerPosition(event.getPacket()).getLocation(user.getPlayer().getWorld()));
        }
    }

    @Override
    public void onPacketReceiving(final PacketEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        // --------------------------------------------- CombatOrder ---------------------------------------------- //

        vlManager.flag(user.getPlayer(), true, this.animationPattern.apply(user, event), -1, () -> {}, () -> {});

        // --------------------------------------------- EqualRotation ---------------------------------------------- //

        vlManager.flag(user.getPlayer(), true, this.equalRotationPattern.apply(user, event), -1, () -> {}, () -> {});
        vlManager.flag(user.getPlayer(), true, this.illegalPitchPattern.apply(user, event), -1, () -> {}, () -> {});

        // ----------------------------------------- Compare + PositionSpoof ---------------------------------------- //
        if (user.getPacketAnalysisData().lastPositionForceData != null) {
            // Special code to update the timestamp of the last compare flag.
            vlManager.flag(user.getPlayer(), true, this.comparePattern.apply(user, event), -1, () -> {}, () -> user.getPacketAnalysisData().updateTimeStamp(0));
            vlManager.flag(user.getPlayer(), true, this.positionSpoofPattern.apply(user, event), -1, () -> {}, () -> {});

            // No continuous flagging.
            user.getPacketAnalysisData().lastPositionForceData = null;
        }
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public Set<Pattern> getPatterns()
    {
        return ImmutableSet.of(animationPattern,
                               comparePattern,
                               equalRotationPattern,
                               illegalPitchPattern,
                               positionSpoofPattern);
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.PACKET_ANALYSIS;
    }
}
