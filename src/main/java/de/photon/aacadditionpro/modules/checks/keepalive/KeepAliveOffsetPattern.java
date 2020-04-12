package de.photon.aacadditionpro.modules.checks.keepalive;

import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.subdata.KeepAliveData;

/**
 * This {@link de.photon.aacadditionpro.modules.PatternModule.Pattern} detects responses to KeepAlive packets which are
 * out of order.
 */
public class KeepAliveOffsetPattern extends PatternModule.Pattern<User, Integer>
{

    @Override
    protected int process(User user, Integer offset)
    {
        synchronized (user.getKeepAliveData().getKeepAlives()) {
            if (user.getKeepAliveData().getKeepAlives().size() == KeepAliveData.KEEPALIVE_QUEUE_SIZE
                // -1 because of size -> index conversion
                && offset > 0)
            {
                message = "PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " sent packets out of order with an offset of: " + offset;
                return Math.min(offset * 2, 10);
            }
        }
        return 0;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.offset";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.KEEPALIVE;
    }
}
