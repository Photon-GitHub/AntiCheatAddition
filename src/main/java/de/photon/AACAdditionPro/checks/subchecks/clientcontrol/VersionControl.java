package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ClientControlModule;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;

import java.io.File;
import java.util.ArrayList;
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

        final YamlConfiguration viaVersionConfig = YamlConfiguration.loadConfiguration(new File("plugins/ViaVersion/config.yml"));
        viaVersionConfig.set("block-disconnect-msg", message);

        final int[] blockedProtocolNumbers = new int[ProtocolVersion.totalVersionNumbers];
        int current = 0;
        for (ProtocolVersion protocolVersion : protocolVersions)
        {
            if (!protocolVersion.allowed)
            {
                for (int versionNumber : protocolVersion.versionNumbers)
                {
                    blockedProtocolNumbers[current++] = versionNumber;
                }
            }
        }

        final int[] trimmedBlockedProtocolNumbers = new int[current];
        System.arraycopy(blockedProtocolNumbers, 0, trimmedBlockedProtocolNumbers, 0, current + 1);

        // Trim the array
        viaVersionConfig.set("block-protocols", trimmedBlockedProtocolNumbers);
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

    private static class ProtocolVersion
    {
        private static int totalVersionNumbers = 0;

        private final String name;
        private final boolean allowed;
        private final int[] versionNumbers;

        private ProtocolVersion(final String name, final int... versionNumbers)
        {
            this.name = name;
            this.allowed = AACAdditionPro.getInstance().getConfig().getBoolean("ClientControl.VersionControl." + this.name);
            this.versionNumbers = versionNumbers;
            totalVersionNumbers += this.versionNumbers.length;
        }
    }
}
