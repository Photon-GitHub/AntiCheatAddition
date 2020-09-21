package de.photon.aacadditionpro.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PacketListenerModule;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.DataKey;
import de.photon.aacadditionpro.user.TimestampKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.packetwrappers.server.WrapperPlayServerPosition;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import lombok.Getter;

import java.util.Set;

public class PacketAnalysis extends PacketAdapter implements PacketListenerModule, ViolationModule
{
    @Getter
    private static final PacketAnalysis instance = new PacketAnalysis();
    private static final Set<Module> submodules = ImmutableSet.of(AnimationPattern.getInstance(),
                                                                  ComparePattern.getInstance(),
                                                                  EqualRotationPattern.getInstance(),
                                                                  IllegalPitchPattern.getInstance(),
                                                                  PositionSpoofPattern.getInstance());

    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 200);


    public PacketAnalysis()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.LOW,
              // --------------- Server --------------- //
              // Compare
              PacketType.Play.Server.POSITION,
              // --------------- Client --------------- //
              // Compare + PositionSpoof
              PacketType.Play.Client.POSITION_LOOK);
    }

    @Override
    public void onPacketSending(PacketEvent event)
    {
        final User user = UserManager.safeGetUserFromPacketEvent(event);

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
        final User user = UserManager.safeGetUserFromPacketEvent(event);

        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        // ----------------------------------------- Compare + PositionSpoof ---------------------------------------- //
        if (user.getDataMap().getValue(DataKey.PACKET_ANALYSIS_LAST_POSITION_FORCE_LOCATION) != null) {
            // Special code to update the timestamp of the last compare flag.
            ComparePattern.getInstance().getApplyingConsumer().accept(user, event);
            PositionSpoofPattern.getInstance().getApplyingConsumer().accept(user, event);

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
    public Set<Module> getSubModules()
    {
        return submodules;
    }

    @Override
    public boolean isSubModule()
    {
        return false;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.PACKET_ANALYSIS;
    }
}
