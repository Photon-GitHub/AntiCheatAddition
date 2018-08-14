package de.photon.AACAdditionPro.modules;

import de.photon.AACAdditionPro.ServerVersion;

import java.util.Set;

public interface RestrictedServerVersionModule extends Module
{
    /**
     * Displays all the {@link ServerVersion}s a check supports.
     * It will autodisable itself on the server start if the server version matches the excluded {@link ServerVersion}.
     */
    Set<ServerVersion> getSupportedVersions();
}
