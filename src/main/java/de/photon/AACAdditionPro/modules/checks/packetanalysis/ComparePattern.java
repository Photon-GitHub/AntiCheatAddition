package de.photon.AACAdditionPro.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;

/**
 * This {@link de.photon.AACAdditionPro.modules.PatternModule.PacketPattern} checks for fluctuating response times of
 * the client.
 * To do this it uses the fact that every {{@link com.comphenix.protocol.PacketType.Play.Server#POSITION}} packet must
 * be answered with an {@link com.comphenix.protocol.PacketType.Play.Client#POSITION_LOOK} packet
 */
class ComparePattern extends PatternModule.PacketPattern
{
    @LoadFromConfiguration(configPath = ".allowed_offset")
    private int allowedOffset;
    @LoadFromConfiguration(configPath = ".compare_threshold")
    private int compareThreshold;
    @LoadFromConfiguration(configPath = ".violation_time")
    private int violationTime;

    ComparePattern()
    {
        // Response to PacketType.Play.Server.POSITION
        super(ImmutableSet.of(PacketType.Play.Client.POSITION_LOOK));
    }

    @Override
    protected int process(User user, PacketContainer packetContainer)
    {
        // Make sure enough datapoints exist for checking.
        if (user.getPacketAnalysisData().getKeepAlives().size() > 10)
        {
            final double offset = MathUtils.offset(
                    user.getPacketAnalysisData().recentKeepAliveResponseTime(),
                    user.getPacketAnalysisData().lastPositionForceData.timeDifference()) - allowedOffset;

            // Should flag
            if (offset > 0)
            {
                // Minimum time between flags to decrease lag spike effects.
                if (!user.getPacketAnalysisData().recentlyUpdated(0, violationTime) &&
                    // Minimum fails to mitigate some fluctuations
                    ++user.getPacketAnalysisData().compareFails > this.compareThreshold)
                {
                    VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " sends packets with different delays.");

                    return Math.min(Math.max(1, (int) (offset / 50)), 12);
                }
            }
            else if (user.getPacketAnalysisData().compareFails > 0)
            {
                user.getPacketAnalysisData().compareFails--;
            }
        }
        return 0;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.Compare";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.PACKET_ANALYSIS;
    }
}
