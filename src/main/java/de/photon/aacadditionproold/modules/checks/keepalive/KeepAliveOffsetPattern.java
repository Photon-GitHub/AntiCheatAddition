package de.photon.aacadditionproold.modules.checks.keepalive;

import de.photon.aacadditionproold.modules.Module;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.modules.RestrictedBungeecord;
import de.photon.aacadditionproold.user.User;
import de.photon.aacadditionproold.user.subdata.KeepAliveData;
import de.photon.aacadditionproold.util.messaging.VerboseSender;
import lombok.Getter;

import java.util.function.BiConsumer;

/**
 * This {@link Module} detects responses to KeepAlive packets which are
 * out of order.
 */
public class KeepAliveOffsetPattern implements Module, RestrictedBungeecord
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
