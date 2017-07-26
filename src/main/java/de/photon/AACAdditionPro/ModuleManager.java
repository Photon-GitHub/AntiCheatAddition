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
        final String verboseName = object.getName().replace(".", ": ");
        try {
            // Enabled in the config
            if (AACAdditionPro.getInstance().getConfig().getBoolean(object.getConfigString() + ".enabled")) {

                // Supports the current server version
                if (object.getSupportedVersions().contains(AACAdditionPro.getInstance().getServerVersion())) {
                    // Enable
                    object.enable();
                    VerboseSender.sendVerboseMessage(verboseName + " has been enabled.", true, false);
                } else {
                    // Auto-Disable as of the wrong server version
                    Bukkit.getScheduler().scheduleSyncDelayedTask(AACAdditionPro.getInstance(), () -> this.remove(object), 1L);
                    VerboseSender.sendVerboseMessage(verboseName + " is not compatible with the server-version.", true, false);
                }
            } else {
                // Disable as it was chosen so in the config
                Bukkit.getScheduler().scheduleSyncDelayedTask(AACAdditionPro.getInstance(), () -> this.remove(object), 1L);
                VerboseSender.sendVerboseMessage(verboseName + " was chosen not to be enabled.", true, false);
            }
        } catch (final Exception e) {
            // Error handling
            VerboseSender.sendVerboseMessage(verboseName + " could not be registered.", true, true);
            e.printStackTrace();
        }
    }

    public List<Module> getManagedObjects()
    {
        return this;
    }
}