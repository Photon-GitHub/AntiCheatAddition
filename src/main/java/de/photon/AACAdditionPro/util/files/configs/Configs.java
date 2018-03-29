package de.photon.AACAdditionPro.util.files.configs;

import lombok.Getter;

import java.io.File;

public enum Configs
{
    AAC("plugins/AAC/config.yml"),
    SPIGOT("spigot.yml"),
    VIAVERSION("plugins/ViaVersion/config.yml");

    @Getter
    private final File configFile;
    @Getter
    private final ConfigurationRepresentation configurationRepresentation;

    Configs(final String path)
    {
        this.configFile = new File(path);
        this.configurationRepresentation = new ConfigurationRepresentation(this.configFile);
    }

    public void updateConfig()
    {
    }
}
