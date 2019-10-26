package de.photon.AACAdditionPro.modules.checks.keepalive;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.user.User;
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
    protected int process(User user, PacketEvent packetEvent)
    {
        final int ignored = user.getKeepAliveData().getIgnoredKeepAlives().getAndSet(0);

        // Only send message on violation.
        if (ignored != 0) {
            VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " ignored " + ignored + "KeepAlive packets");
        }

        return ignored * 10;
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
