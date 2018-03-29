package de.photon.AACAdditionPro.util.files.configs;

import de.photon.AACAdditionPro.exceptions.ConfigException;
import de.photon.AACAdditionPro.util.VerboseSender;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
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
        configChangeRequests.forEach((config, requestedChange) -> config.configuration.set(requestedChange.key, requestedChange.value));
        // Save the config.
        configChangeRequests.keySet().forEach(ExternalConfigs::save);
    }

    public enum ExternalConfigs
    {
        AAC("plugins/AAC/config.yml"),
        SPIGOT("spigot.yml"),
        VIAVERSION("plugins/ViaVersion/config.yml");

        private final File configFile;
        @Getter
        private FileConfig configuration;

        ExternalConfigs(final String path)
        {
            this.configFile = new File(path);
            try
            {
                this.configuration = new FileConfig(this.configFile);
            } catch (ConfigException e)
            {
                VerboseSender.sendVerboseMessage("SEVERE: Failed to load config " + configFile.getName());
                e.printStackTrace();
            }
        }

        private void save()
        {
            try
            {
                this.configuration.save();
            } catch (ConfigException e)
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
