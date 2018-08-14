package de.photon.AACAdditionPro.modules;

import de.photon.AACAdditionPro.ServerVersion;

import java.util.Set;

public interface RestrictedServerVersionModule extends Module
{
    /**
     * Looks up all dependencies to know whether they are loaded.
     *
     * @return <code>true</code> if all dependencies are loaded, otherwise false.
     */
    static boolean allowedToStart(final RestrictedServerVersionModule module)
    {
        return ServerVersion.supportsActiveServerVersion(module.getSupportedVersions());
    }

    /**
     * Displays all the {@link ServerVersion}s a check supports.
     * It will autodisable itself on the server start if the server version matches the excluded {@link ServerVersion}.
     */
    Set<ServerVersion> getSupportedVersions();
}
