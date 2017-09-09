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
     * The server version of the currently running {@link Bukkit} instance.
     */
    @Getter
    private static ServerVersion activeServerVersion;

    static {
        final String versionOutput = Bukkit.getVersion();
        for (final ServerVersion serverVersion : ServerVersion.values()) {
            if (versionOutput.contains(serverVersion.getVersionOutputString())) {
                activeServerVersion = serverVersion;
                // break for better performance as no other version should be found.
                break;
            }
        }
    }
}
