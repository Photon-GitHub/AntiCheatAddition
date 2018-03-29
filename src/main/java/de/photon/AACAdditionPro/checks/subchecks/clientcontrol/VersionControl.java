package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import com.google.common.collect.Sets;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ClientControlModule;
import de.photon.AACAdditionPro.util.files.configs.Configs;
import de.photon.AACAdditionPro.util.multiversion.ServerVersion;
import lombok.Getter;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class VersionControl implements Listener, ClientControlModule
{
    /**
     * Unmodifiable {@link Set} containing all registered {@link ProtocolVersion}s.
     */
    private static final Set<ProtocolVersion> PROTOCOL_VERSIONS;

    static
    {
        PROTOCOL_VERSIONS = Sets.newLinkedHashSetWithExpectedSize(5);
        PROTOCOL_VERSIONS.add(new ProtocolVersion("1.8", ServerVersion.MC188, 47));
        PROTOCOL_VERSIONS.add(new ProtocolVersion("1.9", null, 107, 108, 109, 110));
        PROTOCOL_VERSIONS.add(new ProtocolVersion("1.10", ServerVersion.MC110, 210));
        PROTOCOL_VERSIONS.add(new ProtocolVersion("1.11", ServerVersion.MC111, 315, 316));
        PROTOCOL_VERSIONS.add(new ProtocolVersion("1.12", ServerVersion.MC112, 335, 338, 340));
    }

    /**
     * Method used to get the {@link ServerVersion} from the protocol version number.
     *
     * @param protocolVersion the int returned by {@link us.myles.ViaVersion.api.ViaAPI#getPlayerVersion(Object)} or
     *                        {@link us.myles.ViaVersion.api.ViaAPI#getPlayerVersion(UUID)}
     */
    public static ServerVersion getServerVersionFromProtocolVersion(int protocolVersion)
    {
        for (ProtocolVersion version : PROTOCOL_VERSIONS)
        {
            if (version.versionNumbers.contains(protocolVersion))
            {
                return version.equivalentServerVersion;
            }
        }
        return null;
    }

    @Override
    public void subEnable()
    {
        // Message:
        final Collection<String> versionStrings = new ArrayList<>();
        for (ProtocolVersion protocolVersion : PROTOCOL_VERSIONS)
        {
            if (protocolVersion.allowed)
            {
                versionStrings.add(protocolVersion.name);
            }
        }

        // Get the message
        final String message = AACAdditionPro.getInstance().getConfig().getString("ClientControl.VersionControl.message")
                                             // Replace the special placeholder
                                             .replace("{supportedVersions}", String.join(", ", versionStrings));

        Configs.VIAVERSION.getConfigurationRepresentation().getYamlConfiguration().set("block-disconnect-msg", message);

        // Set the blocked versions
        final List<Integer> blockedProtocolNumbers = new ArrayList<>();
        for (ProtocolVersion protocolVersion : PROTOCOL_VERSIONS)
        {
            if (!protocolVersion.allowed)
            {
                blockedProtocolNumbers.addAll(protocolVersion.versionNumbers);
            }
        }
        Configs.VIAVERSION.getConfigurationRepresentation().getYamlConfiguration().set("block-protocols", blockedProtocolNumbers);
    }

    @Override
    public List<String> getCommandsOnDetection()
    {
        return null;
    }

    @Override
    public Set<String> getDependencies()
    {
        return new HashSet<>(Collections.singletonList("ViaVersion"));
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.VERSION_CONTROL;
    }

    /**
     * Key element for protocol versions.
     */
    @Getter
    private static class ProtocolVersion
    {
        /**
         * The name of the {@link ProtocolVersion}. Intended to be equivalent to minecraft versions.
         * Examples: 1_8, 1_9, 1_10, etc.
         */
        private final String name;

        /**
         * Whether or not this {@link ProtocolVersion} should be allowed to join the server.
         */
        private final boolean allowed;

        /**
         * What {@link ServerVersion} should be used when using this {@link ProtocolVersion}.
         */
        private final ServerVersion equivalentServerVersion;

        /**
         * An unmodifiable {@link List} of {@link Integer}s that contains all protocol version numbers associated with this {@link ProtocolVersion}
         */
        private final Set<Integer> versionNumbers;

        private ProtocolVersion(final String name, final ServerVersion equivalentServerVersion, final Integer... versionNumbers)
        {
            this.name = name;
            this.allowed = AACAdditionPro.getInstance().getConfig().getBoolean("ClientControl.VersionControl.allowedVersions." + this.name);
            this.equivalentServerVersion = equivalentServerVersion;
            this.versionNumbers = Collections.unmodifiableSet(Sets.newHashSet(versionNumbers));
        }
    }
}
