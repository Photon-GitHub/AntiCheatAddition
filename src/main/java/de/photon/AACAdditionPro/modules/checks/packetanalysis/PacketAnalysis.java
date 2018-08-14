package de.photon.AACAdditionPro.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.modules.Module;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PacketListenerModule;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.modules.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.user.data.PacketAnalysisData;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayClientKeepAlive;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerPosition;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;

import java.util.Set;

public class PacketAnalysis extends PacketAdapter implements PacketListenerModule, PatternModule, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 200);

    private PacketPattern comparePattern = new ComparePattern();
    private PacketPattern equalRotationPattern = new EqualRotationPattern();
    private PacketPattern positionSpoofPattern = new PositionSpoofPattern();
    private PacketPattern keepAliveIgnoredPattern = new KeepAliveIgnoredPattern();
    private Pattern<Object, Object> keepAliveInjectPattern = new KeepAliveInjectPattern();

    private boolean keepAlive;
    @LoadFromConfiguration(configPath = ".parts.KeepAlive.unregistered.enabled")
    private boolean keepAliveUnregistered;
    @LoadFromConfiguration(configPath = ".parts.KeepAlive.ignored.enabled")
    private boolean keepAliveIgnored;
    @LoadFromConfiguration(configPath = ".parts.KeepAlive.offset.enabled")
    private boolean keepAliveOffset;
    @LoadFromConfiguration(configPath = ".parts.KeepAlive.inject.enabled")
    private boolean keepAliveInject;

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
        if (User.isUserInvalid(user, this.getModuleType()) || !keepAlive)
        {
            return;
        }

        vlManager.flag(user.getPlayer(), keepAliveIgnoredPattern.apply(user, event.getPacket()), -1, () -> {}, () -> {});

        if (event.getPacketType() == PacketType.Play.Server.POSITION)
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
            if (keepAlive)
            {
                final WrapperPlayClientKeepAlive clientKeepAliveWrapper = new WrapperPlayClientKeepAlive(event.getPacket());
                PacketAnalysisData.KeepAlivePacketData keepAlivePacketData = null;

                int index = user.getPacketAnalysisData().getKeepAlives().size() - 1;
                while (index >= 0)
                {
                    PacketAnalysisData.KeepAlivePacketData alivePacketData = user.getPacketAnalysisData().getKeepAlives().get(index);
                    if (alivePacketData.getKeepAliveID() == clientKeepAliveWrapper.getKeepAliveId())
                    {
                        keepAlivePacketData = alivePacketData;
                        break;
                    }
                    index--;
                }

                // A packet with the same data must have been sent before.
                if (keepAlivePacketData == null)
                {
                    VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " sent unregistered KeepAlive packet.");
                    vlManager.flag(user.getPlayer(), 20, -1, () -> {}, () -> {});
                }
                else
                {
                    keepAlivePacketData.registerResponse();

                    if (keepAliveOffset &&
                        user.getPacketAnalysisData().getKeepAlives().size() == PacketAnalysisData.KEEPALIVE_QUEUE_SIZE)
                    {
                        // -1 because of size -> index conversion
                        final int offset = (PacketAnalysisData.KEEPALIVE_QUEUE_SIZE - 1) - index;
                        if (offset > 0)
                        {
                            VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " sent packets out of order with an offset of: " + offset);
                            vlManager.flag(user.getPlayer(), Math.min((PacketAnalysisData.KEEPALIVE_QUEUE_SIZE - index) * 2, 10), -1, () -> {}, () -> {});
                        }
                    }
                }
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
        keepAlive = keepAliveUnregistered || keepAliveIgnored || keepAliveOffset || keepAliveInject;

        // Unregistered must be enabled to use offset analysis.
        if (keepAlive && !keepAliveUnregistered)
        {
            keepAlive = false;

            Module.disableModule(this.keepAliveIgnoredPattern);
            Module.disableModule(this.keepAliveInjectPattern);

            VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData | Failed to enable KeepAlive part", true, true);
            VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData | In order to use the KeepAlive you need to enable the unregistered analysis!", true, true);
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
        return ImmutableSet.of(comparePattern, equalRotationPattern, positionSpoofPattern, keepAliveInjectPattern);
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.PACKET_ANALYSIS;
    }
}
