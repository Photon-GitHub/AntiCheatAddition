package de.photon.anticheataddition.util.config;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.util.messaging.DebugSender;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

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

    Configs(@NotNull final String path)
    {
        this.configFile = new File(path);
        this.configurationRepresentation = new ConfigurationRepresentation(this.configFile);
    }

    public void saveChanges()
    {
        try {
            this.configurationRepresentation.save();
        } catch (IOException e) {
            DebugSender.getInstance().sendDebug("Unable to change and save" + this.name() + "'s config.", true, true);
            AntiCheatAddition.getInstance().getLogger().log(Level.SEVERE, "Error when saving a config: ", e);
        }
    }
}
