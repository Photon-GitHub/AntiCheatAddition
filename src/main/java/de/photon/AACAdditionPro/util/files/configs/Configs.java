package de.photon.AACAdditionPro.util.files.configs;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.util.VerboseSender;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

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

    public static void saveChangesForAllConfigs()
    {
        for (Configs config : values()) {
            config.saveChanges();
        }
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
