package de.photon.AACAdditionPro.util.files;

import de.photon.AACAdditionPro.util.VerboseSender;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;

public final class ExternalConfigUtils
{
    private static EnumMap<ExternalConfigs, RequestedConfigChange> configChangeRequests = new EnumMap<>(ExternalConfigs.class);

    public static void requestConfigChange(ExternalConfigs externalConfigs, RequestedConfigChange requestedConfigChange)
    {
        configChangeRequests.put(externalConfigs, requestedConfigChange);
    }

    public static void changeConfigs()
    {
        // Set the values
        configChangeRequests.forEach((config, requestedChange) -> config.configYAML.set(requestedChange.key, requestedChange.value));
        // Save the config.
        configChangeRequests.keySet().forEach(ExternalConfigs::saveYAML);
    }

    public enum ExternalConfigs
    {
        AAC("plugins/AAC/config.yml"),
        SPIGOT("spigot.yml"),
        VIAVERSION("plugins/ViaVersion/config.yml");

        private final File configFile;
        @Getter
        private final YamlConfiguration configYAML;

        ExternalConfigs(final String path)
        {
            this.configFile = new File(path);
            this.configYAML = YamlConfiguration.loadConfiguration(this.configFile);
        }

        private void saveYAML()
        {
            try
            {
                this.configYAML.save(configFile);
            } catch (IOException e)
            {
                VerboseSender.sendVerboseMessage("Failed to modify " + this.name() + " config.", true, true);
                e.printStackTrace();
            }

        }
    }

    @AllArgsConstructor
    public static class RequestedConfigChange
    {
        private final String key;
        private final Object value;
    }
}
