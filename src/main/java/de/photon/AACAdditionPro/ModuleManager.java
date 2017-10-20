package de.photon.AACAdditionPro;

import de.photon.AACAdditionPro.util.multiversion.ServerVersion;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;

import java.util.ArrayList;

/**
 * Manages the {@link Module}s of {@link AACAdditionPro}.
 * It extends {@link ArrayList} and thus has an in-built iterator.
 */
public class ModuleManager extends ArrayList<Module>
{
    protected ModuleManager(final Module... initialObjects)
    {
        super(initialObjects.length);
        for (Module initialObject : initialObjects)
        {
            this.registerObject(initialObject);
        }
    }

    private void registerObject(Module object)
    {
        try
        {
            // Save what should be written in the current path (no error) in this variable.
            final String pathOutput;
            // Enabled in the config
            if (AACAdditionPro.getInstance().getConfig().getBoolean(object.getConfigString() + ".enabled"))
            {
                // Supports the current server version
                if (ServerVersion.supportsActiveServerVersion(object.getSupportedVersions()))
                {
                    // Enable
                    this.add(object);
                    object.enable();
                    pathOutput = " has been enabled.";
                }
                else
                {
                    pathOutput = " is not compatible with your server version.";
                }
            }
            else
            {
                // Disable as it was chosen so in the config
                // Do not remove here as one might want to enable the check via the API
                pathOutput = " was chosen not to be enabled.";
            }

            VerboseSender.sendVerboseMessage(object.getName() + pathOutput, true, false);
        } catch (final Exception e)
        {
            // Error handling
            VerboseSender.sendVerboseMessage(object.getName() + " could not be registered.", true, true);
            e.printStackTrace();
        }
    }
}