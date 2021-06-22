package de.photon.aacadditionpro;

import com.google.common.collect.Sets;
import de.photon.aacadditionpro.exception.UnknownMinecraftException;
import de.photon.aacadditionpro.util.protocol.ProtocolVersion;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
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
    MC116("1.16", true),
    MC117("1.17", true);


    public static final Set<ServerVersion> ALL_SUPPORTED_VERSIONS;
    public static final Set<ServerVersion> LEGACY_PLUGIN_MESSAGE_VERSIONS = Sets.immutableEnumSet(MC18, MC19, MC110, MC111, MC112);
    public static final Set<ServerVersion> LEGACY_EVENT_VERSIONS = Sets.immutableEnumSet(MC18, MC19, MC110, MC111, MC112, MC113);
    public static final Set<ServerVersion> NON_188_VERSIONS;
    /**
     * The server version of the currently running {@link Bukkit} instance.
     */
    @Getter
    @NotNull
    private static final ServerVersion activeServerVersion = Arrays.stream(ServerVersion.values())
                                                                   .filter(serverVersion -> Bukkit.getVersion().contains(serverVersion.getVersionOutputString()))
                                                                   .findFirst()
                                                                   .orElseThrow(UnknownMinecraftException::new);

    static {
        val allSup = EnumSet.noneOf(ServerVersion.class);
        for (ServerVersion s : ServerVersion.values()) if (s.supported) allSup.add(s);
        ALL_SUPPORTED_VERSIONS = Sets.immutableEnumSet(allSup);
        allSup.remove(MC18);
        NON_188_VERSIONS = Sets.immutableEnumSet(allSup);
    }

    private final String versionOutputString;
    private final boolean supported;

    /**
     * Used to get the client version. Might only differ from {@link #getActiveServerVersion()} if ViaVersion is installed.
     */
    @NotNull
    public static ServerVersion getClientServerVersion(final Player player)
    {
        if (player == null) return activeServerVersion;
        val viaAPI = AACAdditionPro.getInstance().getViaAPI();
        if (viaAPI == null) return activeServerVersion;

        val clientVersion = ProtocolVersion.getByVersionNumber(viaAPI.getPlayerVersion(player));
        return clientVersion == null ? activeServerVersion : clientVersion.getEquivalentServerVersion();
    }

    /**
     * Used to check whether the current server version is included in a set of supported server versions.
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
