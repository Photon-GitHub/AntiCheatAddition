package de.photon.aacadditionpro.modules.checks.keepalive;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.modules.RestrictedServerVersion;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.packetwrappers.server.WrapperPlayServerKeepAlive;
import de.photon.aacadditionpro.util.random.RandomUtil;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumSet;
import java.util.Set;

/**
 * This {@link de.photon.aacadditionpro.modules.PatternModule.Pattern} randomly injects KeepAlive packets into the
 * regular communication with the client to improve the detection speed of cheats.
 * This is restricted to minecraft 1.8.8 as higher versions have changed the KeepAlive packet handling.
 */
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
                }, RandomUtil.randomBoundaryInt(80, 35));
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

        if (injectTask != null)
        {
            injectTask.cancel();
        }
    }

    @Override
    protected int process(Object o, Object o2)
    {
        throw new UnsupportedOperationException("KeepAliveInject does not process data.");
    }

    @Override
    public Set<ServerVersion> getSupportedVersions()
    {
        return EnumSet.of(ServerVersion.MC188);
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
