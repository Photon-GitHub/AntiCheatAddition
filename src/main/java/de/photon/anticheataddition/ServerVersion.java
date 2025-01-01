package de.photon.anticheataddition;

import de.photon.anticheataddition.exception.UnknownMinecraftException;
import de.photon.anticheataddition.util.datastructure.Pair;
import de.photon.anticheataddition.util.datastructure.SetUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Getter
public enum ServerVersion
{
    // As we compare the versions these MUST be sorted.

    MC18("1.8.8", true, 47),
    MC19("1.9", false, 107, 108, 109, 110),
    MC110("1.10", false, 210),
    MC111("1.11.2", false, 315, 316),
    MC112("1.12.2", true, 335, 338, 340),
    MC113("1.13", false, 393, 401, 404),
    MC114("1.14", false, 477, 480, 485, 490, 498),
    MC115("1.15.2", false, 573, 575),
    MC116("1.16.5", false, 735, 736, 751, 753, 754),
    MC117("1.17.1", true, 755, 756),
    MC118("1.18.2", true, 757, 758),
    MC119("1.19.4", true, 759, 760, 761, 762),
    MC120("1.20", true, 763, 764, 765, 766),
    MC121("1.21", true, 767);

    private static final Map<Integer, ServerVersion> PROTOCOL_VERSION_MAP = Arrays.stream(ServerVersion.values())
                                                                                  // Map each protocol version number to the ServerVersion.
                                                                                  .flatMap(sv -> sv.getProtocolVersions().stream().map(vn -> Pair.of(vn, sv)))
                                                                                  // Create a map.
                                                                                  .collect(Collectors.toUnmodifiableMap(Pair::first, Pair::second));

    public static final Set<ServerVersion> ALL_SUPPORTED_VERSIONS = MC18.getSupVersionsFrom();
    public static final Set<ServerVersion> LEGACY_PLUGIN_MESSAGE_VERSIONS = MC112.getSupVersionsTo();
    public static final ServerVersion NEW_EVENT_VERSION = MC114;
    public static final Set<ServerVersion> NON_188_VERSIONS = MC19.getSupVersionsFrom();

    /**
     * The server version of the currently running {@link Bukkit} instance.
     */
    @NotNull
    public static final ServerVersion ACTIVE = Arrays.stream(ServerVersion.values())
                                                     .filter(serverVersion -> Bukkit.getBukkitVersion().startsWith(serverVersion.getVersionOutputString()))
                                                     .findFirst()
                                                     .orElseThrow(UnknownMinecraftException::new);

    /**
     * This is the string that is searched for on startup.
     * If a minor version like 1.18.2 is specified, servers with 1.18 and 1.18.1 will be considered as unsupported.
     */
    private final String versionOutputString;

    /**
     * Whether AntiCheatAddition still supports that version.
     * If this is false any server of that version will get an error on startup.
     */
    private final boolean supported;

    /**
     * The protocol versions of the major minecraft version.
     * E.g. even though the name is 1.18.2, this shall include the protocol versions of 1.18, 1.18.1 and 1.18.2.
     */
    private final Set<Integer> protocolVersions;

    ServerVersion(String versionOutputString, boolean supported, Integer... protocolVersions)
    {
        this.versionOutputString = versionOutputString;
        this.supported = supported;
        this.protocolVersions = Set.of(protocolVersions);
    }

    /**
     * This method checks if the active server version is equal to or earlier than this version.
     */
    public boolean activeIsEarlierOrEqual()
    {
        return this.compareTo(ACTIVE) >= 0;
    }

    /**
     * This method checks if the active server version is equal to or later than this version.
     */
    public boolean activeIsLaterOrEqual()
    {
        return this.compareTo(ACTIVE) <= 0;
    }

    // Lazy getting as most versions are not supported or used.
    // Also, this is important to avoid loading errors (as the generate methods access values() when not fully loaded)
    /**
     * All supported versions that came before this minecraft server version, including this one.
     */
    @Getter(lazy = true) private final Set<ServerVersion> supVersionsTo = getSupportedVersions(version -> this.compareTo(version) >= 0);
    /**
     * All supported versions that came after this minecraft server version, including this one.
     */
    @Getter(lazy = true) private final Set<ServerVersion> supVersionsFrom = getSupportedVersions(version -> this.compareTo(version) <= 0);

    /**
     * Shorthand for activeServerVersion == MC18.
     * Checks if the current ServerVersion is minecraft 1.8.8.
     * This method reduces both code and improves the maintainability as activeServerVersion is only used by those statements that might need changes for a new version.
     */
    public static boolean is18()
    {
        return ACTIVE == MC18;
    }

    /**
     * Used to check whether the current server version is included in a set of supported server versions.
     *
     * @param supportedServerVersions the {@link Set} of supported server versions of the module
     *
     * @return true if the active server version is included in the provided {@link Set} or false if it is not.
     */
    public static boolean containsActive(Set<ServerVersion> supportedServerVersions)
    {
        return supportedServerVersions.contains(ACTIVE);
    }

    /**
     * This gets the respective {@link ServerVersion} for a version number returned by the {@link com.viaversion.viaversion.api.ViaAPI}
     */
    public static Optional<ServerVersion> getByProtocolVersionNumber(int versionNumber)
    {
        return Optional.ofNullable(PROTOCOL_VERSION_MAP.get(versionNumber));
    }

    private static Set<ServerVersion> getSupportedVersions(Predicate<ServerVersion> filter)
    {
        return Arrays.stream(values())
                     .filter(ServerVersion::isSupported)
                     .filter(filter)
                     .collect(SetUtil.toImmutableEnumSet());
    }
}
