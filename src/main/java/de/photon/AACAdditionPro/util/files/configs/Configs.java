package de.photon.AACAdditionPro.util.files.configs;

import de.photon.AACAdditionPro.util.VerboseSender;
import lombok.Getter;

import java.io.File;
import java.io.IOException;

public enum Configs
{
    AAC("plugins/AAC/config.yml", (byte) 3),
    SPIGOT("spigot.yml", (byte) 2),
    VIAVERSION("plugins/ViaVersion/config.yml", (byte) 3);

    @Getter
    private final File configFile;
    @Getter
    private final ConfigurationRepresentation configurationRepresentation;

    Configs(final String path, final byte depthLevel)
    {
        this.configFile = new File(path);
        this.configurationRepresentation = new ConfigurationRepresentation(this.configFile, depthLevel);
    }

    public void saveChanges()
    {
        try
        {
            this.configurationRepresentation.save();
        } catch (IOException e)
        {
            VerboseSender.sendVerboseMessage("Unable to change and save" + this.name() + "'s config.", true, true);
            e.printStackTrace();
        }
    }
}
