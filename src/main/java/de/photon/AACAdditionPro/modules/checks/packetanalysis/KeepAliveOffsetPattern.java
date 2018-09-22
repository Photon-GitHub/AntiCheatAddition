package de.photon.AACAdditionPro.modules.checks.packetanalysis;

import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.data.PacketAnalysisData;
import de.photon.AACAdditionPro.util.VerboseSender;

/**
 * This {@link de.photon.AACAdditionPro.modules.PatternModule.Pattern} detects responses to KeepAlive packets which are
 * out of order.
 */
public class KeepAliveOffsetPattern extends PatternModule.Pattern<User, Integer>
{
    @Override
    protected int process(User user, Integer offset)
    {
        synchronized (user.getPacketAnalysisData().getKeepAlives())
        {
            if (user.getPacketAnalysisData().getKeepAlives().size() == PacketAnalysisData.KEEPALIVE_QUEUE_SIZE)
            {
                // -1 because of size -> index conversion
                if (offset > 0)
                {
                    VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " sent packets out of order with an offset of: " + offset);
                    return Math.min(offset * 2, 10);
                }
            }
        }
        return 0;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.KeepAlive.offset";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.PACKET_ANALYSIS;
    }
}
