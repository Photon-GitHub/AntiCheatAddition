package de.photon.AACAdditionPro.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.data.PacketAnalysisData;
import de.photon.AACAdditionPro.util.VerboseSender;

/**
 * This {@link de.photon.AACAdditionPro.modules.PatternModule.PacketPattern} flags KeepAlive packets that are ignored by the client.
 */
class KeepAliveIgnoredPattern extends PatternModule.PacketPattern
{
    KeepAliveIgnoredPattern()
    {
        super(ImmutableSet.of(PacketType.Play.Server.KEEP_ALIVE));
    }

    @Override
    protected int process(User user, PacketContainer packetContainer)
    {
        // Check on sending to force the client to respond in a certain time-frame.
        if (user.getPacketAnalysisData().getKeepAlives().size() > PacketAnalysisData.KEEPALIVE_QUEUE_SIZE &&
            !user.getPacketAnalysisData().getKeepAlives().removeFirst().hasRegisteredResponse())
        {
            VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " ignored KeepAlive packet.");
            return 10;
        }
        return 0;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.KeepAlive.ignored";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.PACKET_ANALYSIS;
    }
}
