package de.photon.AACAdditionPro.modules.checks.packetanalysis;

import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ServerVersion;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.modules.RestrictedServerVersion;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerKeepAlive;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;

class KeepAliveInjectPattern extends PatternModule.Pattern<Object, Object> implements RestrictedServerVersion
{
    private BukkitTask injectTask;

    private void recursiveKeepAliveInjection()
    {
        injectTask = Bukkit.getScheduler().runTaskLaterAsynchronously(
                AACAdditionPro.getInstance(),
                () ->
                {
                    long time = System.nanoTime() / 1000000L;
                    for (final User user : UserManager.getUsersUnwrapped())
                    {
                        // Not bypassed
                        if (!user.isBypassed(this.getModuleType()))
                        {
                            final WrapperPlayServerKeepAlive wrapperPlayServerKeepAlive = new WrapperPlayServerKeepAlive();
                            wrapperPlayServerKeepAlive.setKeepAliveId(time);
                            wrapperPlayServerKeepAlive.sendPacket(user.getPlayer());
                        }
                    }
                    recursiveKeepAliveInjection();
                }, MathUtils.randomBoundaryInt(80, 35));
    }

    @Override
    public void enable()
    {
        super.enable();
        this.recursiveKeepAliveInjection();
    }

    @Override
    public void disable()
    {
        super.disable();
        injectTask.cancel();
    }

    @Override
    protected int process(Object o, Object o2)
    {
        throw new UnsupportedOperationException("KeepAliveInject does not process data.");
    }

    @Override
    public Set<ServerVersion> getSupportedVersions()
    {
        return ImmutableSet.of(ServerVersion.MC188);
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.KeepAlive.inject";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.PACKET_ANALYSIS;
    }
}
