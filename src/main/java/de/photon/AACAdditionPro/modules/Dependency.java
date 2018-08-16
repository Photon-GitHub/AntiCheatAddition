package de.photon.AACAdditionPro.modules;

import org.bukkit.Bukkit;

import java.util.Set;

public interface Dependency
{
    /**
     * Looks up all dependencies to know whether they are loaded.
     *
     * @return <code>true</code> if all dependencies are loaded, otherwise false.
     */
    static boolean allowedToStart(final Dependency module)
    {
        return module.getDependencies().stream().allMatch(dependency -> Bukkit.getServer().getPluginManager().isPluginEnabled(dependency));
    }

    /**
     * All plugin names this module depends on.
     */
    Set<String> getDependencies();
}
