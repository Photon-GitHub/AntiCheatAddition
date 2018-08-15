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
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayClientKeepAlive;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerKeepAlive;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerPosition;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;

import java.util.Iterator;
import java.util.Set;

public class PacketAnalysis extends PacketAdapter implements PacketListenerModule, PatternModule, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 200);

    private PacketPattern comparePattern = new ComparePattern();
    private PacketPattern equalRotationPattern = new EqualRotationPattern();
    private PacketPattern positionSpoofPattern = new PositionSpoofPattern();

    private Pattern<User, Integer> keepAliveOffsetPattern = new KeepAliveOffsetPattern();
    private PacketPattern keepAliveIgnoredPattern = new KeepAliveIgnoredPattern();
    private Pattern<Object, Object> keepAliveInjectPattern = new KeepAliveInjectPattern();

    public PacketAnalysis()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.LOW,
              // Compare
              PacketType.Play.Server.POSITION,
              // KeepAlive analysis
              PacketType.Play.Server.KEEP_ALIVE,
              PacketType.Play.Client.KEEP_ALIVE,
              // EqualRotation + Compare
              PacketType.Play.Client.POSITION_LOOK,
              // EqualRotation
              PacketType.Play.Client.LOOK);
    }

    @Override
    public void onPacketSending(PacketEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType()))
        {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.KEEP_ALIVE)
        {
            // Register the KeepAlive
            user.getPacketAnalysisData().getKeepAlives().addLast(new PacketAnalysisData.KeepAlivePacketData(new WrapperPlayServerKeepAlive(event.getPacket()).getKeepAliveId()));
            vlManager.flag(user.getPlayer(), keepAliveIgnoredPattern.apply(user, event.getPacket()), -1, () -> {}, () -> {});
        }
        else if (event.getPacketType() == PacketType.Play.Server.POSITION)
        {
            final WrapperPlayServerPosition serverPositionWrapper = new WrapperPlayServerPosition(event.getPacket());
            user.getPacketAnalysisData().lastPositionForceData = new PacketAnalysisData.PositionForceData(serverPositionWrapper.getLocation(user.getPlayer().getWorld()));
        }
    }

    @Override
    public void onPacketReceiving(final PacketEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType()))
        {
            return;
        }

        // --------------------------------------------- EqualRotation ---------------------------------------------- //

        vlManager.flag(user.getPlayer(), this.equalRotationPattern.apply(user, event.getPacket()), -1, () -> {}, () -> {});

        if (event.getPacketType() == PacketType.Play.Client.KEEP_ALIVE)
        {
            // --------------------------------------------- KeepAlive ---------------------------------------------- //

            final long keepAliveId = new WrapperPlayClientKeepAlive(event.getPacket()).getKeepAliveId();
            PacketAnalysisData.KeepAlivePacketData keepAlivePacketData = null;

            int offset = 0;
            final Iterator<PacketAnalysisData.KeepAlivePacketData> iterator = user.getPacketAnalysisData().getKeepAlives().descendingIterator();
            PacketAnalysisData.KeepAlivePacketData current;
            while (iterator.hasNext())
            {
                current = iterator.next();

                if (current.getKeepAliveID() == keepAliveId)
                {
                    keepAlivePacketData = current;
                    break;
                }

                offset++;
            }

            // A packet with the same data must have been sent before.
            if (keepAlivePacketData == null ||
                // If the packet already has a response something is off.
                keepAlivePacketData.hasRegisteredResponse())
            {
                VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " sent unregistered KeepAlive packet.");
                vlManager.flag(user.getPlayer(), 20, -1, () -> {}, () -> {});
            }
            else
            {
                keepAlivePacketData.registerResponse();
                vlManager.flag(user.getPlayer(), keepAliveOffsetPattern.apply(user, offset), -1, () -> {}, () -> {});
            }
        }

        // ----------------------------------------- Compare + PositionSpoof ---------------------------------------- //
        if (user.getPacketAnalysisData().lastPositionForceData != null)
        {
            // Special code to update the timestamp of the last compare flag.
            vlManager.flag(user.getPlayer(), this.comparePattern.apply(user, event.getPacket()), -1, () -> {}, () -> user.getPacketAnalysisData().updateTimeStamp(0));
            vlManager.flag(user.getPlayer(), this.positionSpoofPattern.apply(user, event.getPacket()), -1, () -> {}, () -> {});

            // No continuous flagging.
            user.getPacketAnalysisData().lastPositionForceData = null;
        }
    }

    @Override
    public void enable()
    {
        PatternModule.enablePatterns(this);
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public Set<Pattern> getPatterns()
    {
        return ImmutableSet.of(comparePattern, equalRotationPattern, positionSpoofPattern, keepAliveOffsetPattern, keepAliveIgnoredPattern, keepAliveInjectPattern);
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.PACKET_ANALYSIS;
    }
}
