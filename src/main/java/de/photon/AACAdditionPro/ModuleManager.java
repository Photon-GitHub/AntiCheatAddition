package de.photon.AACAdditionPro;

import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class ModuleManager extends ArrayList<Module>
{
    protected ModuleManager(final Module... initialObjects)
    {
        super(Arrays.asList(initialObjects));
        this.forEach(this::registerObject);
    }

    protected void registerObject(Module object)
    {
        try {
            // Save what should be written in the current path (no error) in this variable.
            final String pathOutput;
            // Enabled in the config
            if (AACAdditionPro.getInstance().getConfig().getBoolean(object.getConfigString() + ".enabled")) {

                // Supports the current server version
                if (object.getSupportedVersions().contains(AACAdditionPro.getInstance().getServerVersion())) {
                    // Enable
                    object.enable();
                    pathOutput = " has been enabled.";
                } else {
                    // Auto-Disable as of the wrong server version
                    Bukkit.getScheduler().scheduleSyncDelayedTask(AACAdditionPro.getInstance(), () -> this.remove(object), 1L);
                    pathOutput = " is not compatible with the server-version.";
                }
            } else {
                // Disable as it was chosen so in the config
                Bukkit.getScheduler().scheduleSyncDelayedTask(AACAdditionPro.getInstance(), () -> this.remove(object), 1L);
                pathOutput = " was chosen not to be enabled.";
            }

            VerboseSender.sendVerboseMessage(object.getName() + pathOutput, true, false);
        } catch (final Exception e) {
            // Error handling
            VerboseSender.sendVerboseMessage(object.getName() + " could not be registered.", true, true);
            e.printStackTrace();
        }
    }

    public List<Module> getManagedObjects()
    {
        return this;
    }
}