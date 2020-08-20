package de.photon.aacadditionpro.modules;

import org.bukkit.Bukkit;

import java.util.Set;
import java.util.stream.Collectors;

public interface IncompatiblePluginModule extends Module
{
    /**
     * Looks up all dependencies to know whether they are loaded.
     *
     * @return <code>true</code> if all dependencies are loaded, otherwise false.
     */
    static boolean allowedToStart(final IncompatiblePluginModule module)
    {
        return module.getIncompatiblePlugins().stream().noneMatch(dependency -> Bukkit.getServer().getPluginManager().isPluginEnabled(dependency));
    }

    /**
     * Lists all incompatible plugins.
     */
    static String listInstalledIncompatiblePlugins(final IncompatiblePluginModule module)
    {
        return module.getIncompatiblePlugins().stream().filter(dependency -> Bukkit.getServer().getPluginManager().isPluginEnabled(dependency)).collect(Collectors.joining());
    }


    /**
     * All plugin names this module depends on.
     */
    Set<String> getIncompatiblePlugins();
}
