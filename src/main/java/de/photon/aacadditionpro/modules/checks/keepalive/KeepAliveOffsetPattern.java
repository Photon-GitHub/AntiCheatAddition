package de.photon.aacadditionpro.modules.checks.keepalive;

import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.subdata.KeepAliveData;
import de.photon.aacadditionpro.util.messaging.VerboseSender;
import lombok.Getter;

import java.util.function.BiConsumer;

/**
 * This {@link de.photon.aacadditionpro.modules.Module} detects responses to KeepAlive packets which are
 * out of order.
 */
public class KeepAliveOffsetPattern implements Module
{
    @Getter
    private static final KeepAliveOffsetPattern instance = new KeepAliveOffsetPattern();
    @Getter
    private BiConsumer<User, Integer> applyingConsumer = (user, offset) -> {};

    @Override
    public void enable()
    {
        this.applyingConsumer = (user, offset) -> {
            synchronized (user.getKeepAliveData().getKeepAlives()) {
                if (user.getKeepAliveData().getKeepAlives().size() == KeepAliveData.KEEPALIVE_QUEUE_SIZE
                    // -1 because of size -> index conversion
                    && offset > 0)
                {
                    KeepAlive.getInstance().getViolationLevelManagement().flag(user.getPlayer(), Math.min(offset * 2, 10),
                                                                               -1, () -> {},
                                                                               () -> VerboseSender.getInstance().sendVerboseMessage("PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " sent packets out of order with an offset of: " + offset));
                }
            }
        };
    }

    @Override
    public void disable()
    {
        this.applyingConsumer = (user, offset) -> {};
    }

    @Override
    public boolean isSubModule()
    {
        return true;
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
