package de.photon.aacadditionpro.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.mathematics.MathUtils;

/**
 * This {@link de.photon.aacadditionpro.modules.PatternModule.PacketPattern} checks for fluctuating response times of
 * the client.
 * To do this it uses the fact that every {{@link com.comphenix.protocol.PacketType.Play.Server#POSITION}} packet must
 * be answered with an {@link com.comphenix.protocol.PacketType.Play.Client#POSITION_LOOK} packet
 */
class ComparePattern extends PatternModule.PacketPattern
{
    @LoadFromConfiguration(configPath = ".allowed_offset")
    private int allowedOffset;
    @LoadFromConfiguration(configPath = ".violation_threshold")
    private int violationThreshold;
    @LoadFromConfiguration(configPath = ".violation_time")
    private int violationTime;

    ComparePattern()
    {
        // Response to PacketType.Play.Server.POSITION
        super(ImmutableSet.of(PacketType.Play.Client.POSITION_LOOK));
    }

    @Override
    protected int process(User user, PacketEvent packetEvent)
    {
        final double offset;
        try {
            offset = MathUtils.offset(
                    user.getKeepAliveData().recentKeepAliveResponseTime(),
                    user.getPacketAnalysisData().lastPositionForceData.timeDifference()) - allowedOffset;
            // recentKeepAliveResponseTime() might throw an IllegalStateException if there are not enough answered
            // KeepAlive packets in the queue.
        } catch (IllegalStateException tooFewDataPoints) {
            return 0;
        }

        // Should flag
        if (offset > 0) {
            // Minimum time between flags to decrease lag spike effects.
            if (!user.getPacketAnalysisData().recentlyUpdated(0, violationTime) &&
                // Minimum fails to mitigate some fluctuations
                ++user.getPacketAnalysisData().compareFails >= this.violationThreshold)
            {
                message = "PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " sends packets with different delays. Mitigated Offset: " + offset;
                return Math.min(Math.max(1, (int) (offset / 50)), 9);
            }
        }
        else if (user.getPacketAnalysisData().compareFails > 0) {
            user.getPacketAnalysisData().compareFails--;
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
