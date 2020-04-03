package de.photon.aacadditionpro.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.user.DataKey;
import de.photon.aacadditionpro.user.TimestampKey;
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
                    user.getTimestampMap().passedTime(TimestampKey.PACKET_ANALYSIS_LAST_POSITION_FORCE) - allowedOffset);
            // recentKeepAliveResponseTime() might throw an IllegalStateException if there are not enough answered
            // KeepAlive packets in the queue.
        } catch (IllegalStateException tooFewDataPoints) {
            return 0;
        }

        // Should flag
        if (offset > 0) {
            // Minimum time between flags to decrease lag spike effects.
            if (!user.getTimestampMap().recentlyUpdated(TimestampKey.PACKET_ANALYSIS_LAST_COMPARE_FLAG, violationTime)) {

                // Increment fails.
                final long incrementFails = user.getDataMap().getLong(DataKey.PACKET_ANALYSIS_COMPARE_FAILS) + 1;
                user.getDataMap().setValue(DataKey.PACKET_ANALYSIS_COMPARE_FAILS, incrementFails);

                // Minimum fails to mitigate some fluctuations
                if (incrementFails >= this.violationThreshold) {
                    message = "PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " sends packets with different delays. Mitigated Offset: " + offset;
                    return Math.min(Math.max(1, (int) (offset / 50)), 9);
                }
            }
        } else if (user.getDataMap().getLong(DataKey.PACKET_ANALYSIS_COMPARE_FAILS) > 0) {
            final long decrementFails = user.getDataMap().getLong(DataKey.PACKET_ANALYSIS_COMPARE_FAILS) - 1;
            user.getDataMap().setValue(DataKey.PACKET_ANALYSIS_COMPARE_FAILS, decrementFails);
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
