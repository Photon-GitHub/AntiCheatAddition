package de.photon.anticheataddition;

import com.google.common.collect.Sets;
import de.photon.anticheataddition.exception.UnknownMinecraftException;
import de.photon.anticheataddition.protocol.ProtocolVersion;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Getter
public enum ServerVersion
{
    // As we compare the versions these MUST be sorted.

    MC18("1.8.8", true),
    MC19("1.9", false),
    MC110("1.10", false),
    MC111("1.11.2", false),
    MC112("1.12.2", true),
    MC113("1.13", false),
    MC114("1.14", false),
    MC115("1.15.2", true),
    MC116("1.16.5", true),
    MC117("1.17.1", true),
    MC118("1.18", true);


    public static final Set<ServerVersion> ALL_SUPPORTED_VERSIONS = MC18.getSupVersionsFrom();
    public static final Set<ServerVersion> LEGACY_PLUGIN_MESSAGE_VERSIONS = MC112.getSupVersionsTo();
    public static final Set<ServerVersion> LEGACY_EVENT_VERSIONS = MC113.getSupVersionsTo();
    public static final Set<ServerVersion> NON_188_VERSIONS = MC19.getSupVersionsFrom();

    /**
     * The server version of the currently running {@link Bukkit} instance.
     */
    @Getter
    @NotNull
    private static final ServerVersion activeServerVersion = Arrays.stream(ServerVersion.values())
                                                                   .filter(serverVersion -> Bukkit.getVersion().contains(serverVersion.getVersionOutputString()))
                                                                   .findFirst()
                                                                   .orElseThrow(UnknownMinecraftException::new);

    private final String versionOutputString;
    private final boolean supported;

    // Lazy getting as most versions are not supported or used.
    @Getter(lazy = true) private final Set<ServerVersion> supVersionsTo = generateVersionsTo();
    @Getter(lazy = true) private final Set<ServerVersion> supVersionsFrom = generateVersionsFrom();


    /**
     * Shorthand for activeServerVersion == MC18.
     * Checks if the current ServerVersion is minecraft 1.8.8.
     * This method reduces both code and improves the maintainability as activeServerVersion is only used by those statements that might need changes for a new version.
     */
    public static boolean is18()
    {
        return activeServerVersion == MC18;
    }

    /**
     * Used to get the client version. Might only differ from {@link #getActiveServerVersion()} if ViaVersion is installed.
     */
    @NotNull
    public static ServerVersion getClientServerVersion(final Player player)
    {
        if (player == null) return activeServerVersion;
        val viaAPI = AntiCheatAddition.getInstance().getViaAPI();
        if (viaAPI == null) return activeServerVersion;

        val clientVersion = ProtocolVersion.getByVersionNumber(viaAPI.getPlayerVersion(player.getUniqueId()));
        return clientVersion == null ? activeServerVersion : clientVersion.getEquivalentServerVersion();
    }

    /**
     * Used to check whether the current server version is included in a set of supported server versions.
     *
     * @param supportedServerVersions the {@link Set} of supported server versions of the module
     *
     * @return true if the active server version is included in the provided {@link Set} or false if it is not.
     */
    public static boolean containsActiveServerVersion(Set<ServerVersion> supportedServerVersions)
    {
        return supportedServerVersions.contains(activeServerVersion);
    }

    private Set<ServerVersion> generateVersionsTo()
    {
        return Sets.immutableEnumSet(Arrays.stream(values())
                                           .filter(ServerVersion::isSupported)
                                           .filter(version -> this.compareTo(version) >= 0)
                                           .collect(Collectors.toCollection(() -> EnumSet.noneOf(ServerVersion.class))));
    }

    private Set<ServerVersion> generateVersionsFrom()
    {
        return Sets.immutableEnumSet(Arrays.stream(values())
                                           .filter(ServerVersion::isSupported)
                                           .filter(version -> this.compareTo(version) <= 0)
                                           .collect(Collectors.toCollection(() -> EnumSet.noneOf(ServerVersion.class))));
    }
}
