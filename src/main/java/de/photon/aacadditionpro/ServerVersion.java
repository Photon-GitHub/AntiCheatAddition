package de.photon.aacadditionpro;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;

import java.util.EnumSet;
import java.util.Set;

@RequiredArgsConstructor
@Getter
public enum ServerVersion
{
    MC18("1.8.8", true),
    MC19("1.9", false),
    MC110("1.10", false),
    MC111("1.11.2", false),
    MC112("1.12.2", true),
    MC113("1.13.2", true),
    MC114("1.14.4", true),
    MC115("1.15.2", true),
    MC116("1.16", true);


    public static final Set<ServerVersion> ALL_SUPPORTED_VERSIONS;
    public static final Set<ServerVersion> LEGACY_PLUGIN_MESSAGE_VERSIONS = EnumSet.of(MC18);
    public static final Set<ServerVersion> LEGACY_EVENT_VERSIONS = EnumSet.of(MC18, MC19, MC110, MC111, MC112, MC113);
    public static final Set<ServerVersion> NON_188_VERSIONS;
    /**
     * The server version of the currently running {@link Bukkit} instance.
     */
    @Getter
    private static ServerVersion activeServerVersion;

    static {
        ALL_SUPPORTED_VERSIONS = EnumSet.allOf(ServerVersion.class);
        ALL_SUPPORTED_VERSIONS.removeIf(serverVersion -> !serverVersion.supported);

        NON_188_VERSIONS = EnumSet.copyOf(ALL_SUPPORTED_VERSIONS);
        NON_188_VERSIONS.remove(MC18);

        final String versionOutput = Bukkit.getVersion();
        for (final ServerVersion serverVersion : ServerVersion.values()) {
            if (versionOutput.contains(serverVersion.getVersionOutputString())) {
                activeServerVersion = serverVersion;
                // break for better performance as no other version should be found.
                break;
            }
        }
    }

    private final String versionOutputString;
    private final boolean supported;

    /**
     * Used to check whether the current server version is included in the supported server versions of a {@link Module}
     *
     * @param supportedServerVersions the {@link Set} of supported server versions of the module
     *
     * @return true if the active server version is included in the provided {@link Set} or false if it is not.
     */
    public static boolean supportsActiveServerVersion(Set<ServerVersion> supportedServerVersions)
    {
        return supportedServerVersions.contains(activeServerVersion);
    }
}
