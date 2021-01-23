package de.photon.aacadditionproold.modules;

import org.bukkit.Bukkit;

import java.util.Set;
import java.util.stream.Collectors;

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
     * Lists all missing dependencies.
     */
    static String listMissingDependencies(final Dependency module)
    {
        return module.getDependencies().stream().filter(dependency -> !Bukkit.getServer().getPluginManager().isPluginEnabled(dependency)).collect(Collectors.joining());
    }

    /**
     * All plugin names this module depends on.
     */
    Set<String> getDependencies();
}
