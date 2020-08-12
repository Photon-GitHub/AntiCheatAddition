package de.photon.aacadditionpro.modules.checks.keepalive;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.RestrictedServerVersion;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.packetwrappers.server.WrapperPlayServerKeepAlive;
import de.photon.aacadditionpro.util.random.RandomUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumSet;
import java.util.Set;

/**
 * This {@link Module} randomly injects KeepAlive packets into the
 * regular communication with the client to improve the detection speed of cheats.
 * This is restricted to minecraft 1.8.8 as higher versions have changed the KeepAlive packet handling.
 */
class KeepAliveInjectPattern implements Module, RestrictedServerVersion
{
    @Getter
    private static final KeepAliveInjectPattern instance = new KeepAliveInjectPattern();


    private BukkitTask injectTask;

    private void recursiveKeepAliveInjection()
    {
        injectTask = Bukkit.getScheduler().runTaskLaterAsynchronously(
                AACAdditionPro.getInstance(),
                () ->
                {
                    final long time = System.nanoTime() / 1000000L;

                    WrapperPlayServerKeepAlive wrapperPlayServerKeepAlive;
                    for (final User user : UserManager.getUsersUnwrapped()) {
                        // Not bypassed
                        if (!user.isBypassed(this.getModuleType())) {
                            wrapperPlayServerKeepAlive = new WrapperPlayServerKeepAlive();
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
        this.recursiveKeepAliveInjection();
    }

    @Override
    public void disable()
    {
        if (injectTask != null) {
            injectTask.cancel();
        }
    }

    @Override
    public boolean isSubModule()
    {
        return true;
    }

    @Override
    public Set<ServerVersion> getSupportedVersions()
    {
        return EnumSet.of(ServerVersion.MC188);
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.inject";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.KEEPALIVE;
    }
}
