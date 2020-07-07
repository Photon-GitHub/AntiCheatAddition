package de.photon.aacadditionpro.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PacketListenerModule;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.DataKey;
import de.photon.aacadditionpro.user.TimestampKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.packetwrappers.server.WrapperPlayServerPosition;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;

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
        if (event.isPlayerTemporary()) {
            return;
        }

        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.POSITION) {
            final WrapperPlayServerPosition newPositionWrapper = new WrapperPlayServerPosition(event.getPacket());

            // Ignore relative teleports.
            if (!newPositionWrapper.getFlags().isEmpty()) {
                return;
            }

            user.getDataMap().setValue(DataKey.PACKET_ANALYSIS_LAST_POSITION_FORCE_LOCATION, newPositionWrapper.getLocation(user.getPlayer().getWorld()));
            user.getTimestampMap().updateTimeStamp(TimestampKey.PACKET_ANALYSIS_LAST_POSITION_FORCE);
        }
    }

    @Override
    public void onPacketReceiving(final PacketEvent event)
    {
        final User user = PacketListenerModule.safeGetUserFromEvent(event);

        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        // --------------------------------------------- CombatOrder ---------------------------------------------- //

        vlManager.flag(user.getPlayer(), this.animationPattern.apply(user, event), -1, () -> {}, () -> {});

        // --------------------------------------------- EqualRotation ---------------------------------------------- //

        vlManager.flag(user.getPlayer(), this.equalRotationPattern.apply(user, event), -1, () -> {}, () -> {});
        vlManager.flag(user.getPlayer(), this.illegalPitchPattern.apply(user, event), -1, () -> {}, () -> {});

        // ----------------------------------------- Compare + PositionSpoof ---------------------------------------- //
        if (user.getDataMap().getValue(DataKey.PACKET_ANALYSIS_LAST_POSITION_FORCE_LOCATION) != null) {
            // Special code to update the timestamp of the last compare flag.
            vlManager.flag(user.getPlayer(), this.comparePattern.apply(user, event), -1, () -> {}, () -> user.getTimestampMap().updateTimeStamp(TimestampKey.PACKET_ANALYSIS_LAST_COMPARE_FLAG));
            vlManager.flag(user.getPlayer(), this.positionSpoofPattern.apply(user, event), -1, () -> {}, () -> {});

            // No continuous flagging.
            user.getDataMap().setValue(DataKey.PACKET_ANALYSIS_LAST_POSITION_FORCE_LOCATION, null);
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
