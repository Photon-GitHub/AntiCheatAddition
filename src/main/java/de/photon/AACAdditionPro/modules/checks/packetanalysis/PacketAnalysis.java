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
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.packetwrappers.client.WrapperPlayClientKeepAlive;
import de.photon.AACAdditionPro.util.packetwrappers.server.WrapperPlayServerKeepAlive;
import de.photon.AACAdditionPro.util.packetwrappers.server.WrapperPlayServerPosition;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;

import java.util.Iterator;
import java.util.Set;

public class PacketAnalysis extends PacketAdapter implements PacketListenerModule, PatternModule, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 200);

    private final ComparePattern comparePattern = new ComparePattern();
    private final EqualRotationPattern equalRotationPattern = new EqualRotationPattern();

    private final IllegalPitchPattern illegalPitchPattern = new IllegalPitchPattern();

    private final KeepAliveOffsetPattern keepAliveOffsetPattern = new KeepAliveOffsetPattern();
    private final KeepAliveIgnoredPattern keepAliveIgnoredPattern = new KeepAliveIgnoredPattern();
    private final KeepAliveInjectPattern keepAliveInjectPattern = new KeepAliveInjectPattern();

    private final PositionSpoofPattern positionSpoofPattern = new PositionSpoofPattern();

    public PacketAnalysis()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.LOW,
              // --------------- Server --------------- //
              // KeepAlive analysis
              PacketType.Play.Server.KEEP_ALIVE,
              // Compare
              PacketType.Play.Server.POSITION,
              // --------------- Client --------------- //
              PacketType.Play.Client.KEEP_ALIVE,
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

        if (event.getPacketType() == PacketType.Play.Server.KEEP_ALIVE) {
            // Register the KeepAlive
            synchronized (user.getPacketAnalysisData().getKeepAlives()) {
                user.getPacketAnalysisData().getKeepAlives().add(new PacketAnalysisData.KeepAlivePacketData(new WrapperPlayServerKeepAlive(event.getPacket()).getKeepAliveId()));
            }
            vlManager.flag(user.getPlayer(), keepAliveIgnoredPattern.apply(user, event), -1, () -> {}, () -> {});
        }
        else if (event.getPacketType() == PacketType.Play.Server.POSITION) {
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

        // --------------------------------------------- EqualRotation ---------------------------------------------- //

        vlManager.flag(user.getPlayer(), this.equalRotationPattern.apply(user, event), -1, () -> {}, () -> {});
        vlManager.flag(user.getPlayer(), this.illegalPitchPattern.apply(user, event), -1, () -> {}, () -> {});

        if (event.getPacketType() == PacketType.Play.Client.KEEP_ALIVE) {
            // --------------------------------------------- KeepAlive ---------------------------------------------- //

            final long keepAliveId = new WrapperPlayClientKeepAlive(event.getPacket()).getKeepAliveId();
            PacketAnalysisData.KeepAlivePacketData keepAlivePacketData = null;

            int offset = 0;
            synchronized (user.getPacketAnalysisData().getKeepAlives()) {
                final Iterator<PacketAnalysisData.KeepAlivePacketData> iterator = user.getPacketAnalysisData().getKeepAlives().descendingIterator();
                PacketAnalysisData.KeepAlivePacketData current;
                while (iterator.hasNext()) {
                    current = iterator.next();

                    if (current.getKeepAliveID() == keepAliveId) {
                        keepAlivePacketData = current;
                        break;
                    }

                    offset++;
                }
            }

            // A packet with the same data must have been sent before.
            if (keepAlivePacketData == null ||
                // If the packet already has a response something is off.
                keepAlivePacketData.hasRegisteredResponse())
            {
                VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " sent unregistered KeepAlive packet.");
                vlManager.flag(user.getPlayer(), 20, -1, () -> {}, () -> {});
            }
            else {
                keepAlivePacketData.registerResponse();
                vlManager.flag(user.getPlayer(), keepAliveOffsetPattern.apply(user, offset), -1, () -> {}, () -> {});
            }
        }

        // ----------------------------------------- Compare + PositionSpoof ---------------------------------------- //
        if (user.getPacketAnalysisData().lastPositionForceData != null) {
            // Special code to update the timestamp of the last compare flag.
            vlManager.flag(user.getPlayer(), this.comparePattern.apply(user, event), -1, () -> {}, () -> user.getPacketAnalysisData().updateTimeStamp(0));
            vlManager.flag(user.getPlayer(), this.positionSpoofPattern.apply(user, event), -1, () -> {}, () -> {});

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
        return ImmutableSet.of(comparePattern,
                               equalRotationPattern,
                               illegalPitchPattern,
                               keepAliveOffsetPattern,
                               keepAliveIgnoredPattern,
                               keepAliveInjectPattern,
                               positionSpoofPattern);
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.PACKET_ANALYSIS;
    }
}
