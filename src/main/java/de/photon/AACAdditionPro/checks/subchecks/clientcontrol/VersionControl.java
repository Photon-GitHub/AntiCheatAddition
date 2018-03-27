package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ClientControlModule;
import de.photon.AACAdditionPro.util.VerboseSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VersionControl implements Listener, ClientControlModule
{
    public VersionControl()
    {
        final HashSet<ProtocolVersion> protocolVersions = new HashSet<>();
        // Register all versions
        protocolVersions.add(new ProtocolVersion("1.8", 47));
        protocolVersions.add(new ProtocolVersion("1.9", 107, 108, 109, 110));
        protocolVersions.add(new ProtocolVersion("1.10", 210));
        protocolVersions.add(new ProtocolVersion("1.11", 315, 316));
        protocolVersions.add(new ProtocolVersion("1.12", 335, 338, 340));

        // Message:
        final Collection<String> versionStrings = new ArrayList<>();
        for (ProtocolVersion protocolVersion : protocolVersions)
        {
            versionStrings.add(protocolVersion.name);
        }

        // Get the message
        final String message = AACAdditionPro.getInstance().getConfig().getString("ClientControl.VersionControl.message")
                                             // Replace the special placeholder
                                             .replace("{supportedVersions}", String.join(", ", versionStrings));

        final File viaVersionFile = new File("plugins/ViaVersion/config.yml");
        final YamlConfiguration viaVersionConfig = YamlConfiguration.loadConfiguration(viaVersionFile);
        viaVersionConfig.set("block-disconnect-msg", message);

        final List<Integer> blockedProtocolNumbers = new ArrayList<>();
        for (ProtocolVersion protocolVersion : protocolVersions)
        {
            if (!protocolVersion.allowed)
            {
                blockedProtocolNumbers.addAll(protocolVersion.versionNumbers);
            }
        }

        viaVersionConfig.set("block-protocols", blockedProtocolNumbers);
        try
        {
            viaVersionConfig.save(viaVersionFile);
        } catch (IOException e)
        {
            VerboseSender.sendVerboseMessage("Failed to modify ViaVersion config.", true, true);
            e.printStackTrace();
        }
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
    private static class ProtocolVersion
    {
        /**
         * The name of the {@link ProtocolVersion}. Intended to be equivalent to minecraft versions.
         * Examples: 1.8, 1.9, 1.10, etc.
         */
        private final String name;
        /**
         * Whether or not this {@link ProtocolVersion} should be allowed to join the server.
         */
        private final boolean allowed;
        /**
         * An unmodifiable {@link List} of {@link Integer}s that contains all protocol version numbers associated with this {@link ProtocolVersion}
         */
        private final List<Integer> versionNumbers;

        private ProtocolVersion(final String name, final Integer... versionNumbers)
        {
            this.name = name;
            this.allowed = AACAdditionPro.getInstance().getConfig().getBoolean("ClientControl.VersionControl." + this.name);
            this.versionNumbers = Collections.unmodifiableList(Arrays.asList(versionNumbers));
        }
    }
}
