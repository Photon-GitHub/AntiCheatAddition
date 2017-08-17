package de.photon.AACAdditionPro.util.multiversion;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;

@RequiredArgsConstructor(suppressConstructorProperties = true)
public enum ServerVersion
{
    MC188("1.8.8"),
    MC110("1.10"),
    MC111("1.11"),
    MC112("1.12");

    @Getter
    private final String versionOutputString;

    /**
     * Identifies the server version of the currently running {@link Bukkit} instance by its version - {@link String}
     *
     * @return the {@link ServerVersion} that fits represents the server's version or
     *
     * @throws IllegalArgumentException if no supported server version was found.
     */
    public static ServerVersion getServerVersion() throws IllegalArgumentException
    {
        final String versionOutput = Bukkit.getVersion();
        for (final ServerVersion serverVersion : ServerVersion.values()) {
            if (versionOutput.contains(serverVersion.getVersionOutputString())) {
                return serverVersion;
            }
        }

        throw new IllegalArgumentException("Unsupported server version");
    }
}
