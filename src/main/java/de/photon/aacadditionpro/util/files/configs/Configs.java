package de.photon.aacadditionpro.util.files.configs;

import de.photon.aacadditionproold.AACAdditionPro;
import de.photon.aacadditionproold.util.messaging.VerboseSender;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

@Getter
public enum Configs
{
    AAC("plugins/AAC/config.yml"),
    SPIGOT("spigot.yml"),
    VIAVERSION("plugins/ViaVersion/config.yml");

    private final File configFile;
    private final ConfigurationRepresentation configurationRepresentation;

    Configs(final String path)
    {
        this.configFile = new File(path);
        this.configurationRepresentation = new ConfigurationRepresentation(this.configFile);
    }

    public static void saveChangesForAllConfigs()
    {
        for (Configs config : values()) config.saveChanges();
    }

    public void saveChanges()
    {
        try {
            this.configurationRepresentation.save();
        } catch (IOException e) {
            VerboseSender.getInstance().sendVerboseMessage("Unable to change and save" + this.name() + "'s config.", true, true);
            AACAdditionPro.getInstance().getLogger().log(Level.SEVERE, "Error when saving a config: ", e);
        }
    }
}
