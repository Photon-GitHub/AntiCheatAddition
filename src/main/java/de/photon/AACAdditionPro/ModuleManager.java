package de.photon.AACAdditionPro;

import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.exceptions.NoViolationLevelManagementException;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.files.configs.Configs;
import de.photon.AACAdditionPro.util.multiversion.ServerVersion;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;
import org.bukkit.Bukkit;

import java.util.ArrayList;

/**
 * Manages the {@link Module}s of {@link AACAdditionPro}.
 * It extends {@link ArrayList} and thus has an in-built iterator.
 */
public class ModuleManager extends ArrayList<Module>
{
    ModuleManager(final Module... initialObjects)
    {
        super(initialObjects.length);
        for (Module initialObject : initialObjects)
        {
            this.registerObject(initialObject);
        }

        // Invoke the changing of configs after all enable calls.
        for (Configs config : Configs.values())
        {
            config.updateConfig();
        }
    }

    public void registerObject(Module object)
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
                    if (object.getDependencies().stream().allMatch(dependency -> Bukkit.getServer().getPluginManager().isPluginEnabled(dependency)))
                    {
                        // Enable
                        this.add(object);

                        object.enable();
                        pathOutput = " has been enabled.";
                    }
                    else
                    {
                        pathOutput = " has been not been enabled as of missing dependencies.";
                    }
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

    /**
     * Gets a {@link Module} from its {@link ModuleType}.
     *
     * @param moduleType the {@link ModuleType} of the {@link Module} that should be found
     *
     * @return the {@link Module} if a match was found
     *
     * @throws IllegalArgumentException if the provided {@link ModuleType} parameter is not used in a {@link Module}
     */
    public Module getModule(final ModuleType moduleType)
    {
        for (final Module module : this)
        {
            if (module.getModuleType() == moduleType)
            {
                return module;
            }
        }
        throw new IllegalArgumentException("The ModuleType: " + moduleType.name() + " is not used in any registered check (is the server version compatible with it?).");
    }

    /**
     * Gets a the {@link ViolationLevelManagement} of a module from its {@link ModuleType}.
     *
     * @param moduleType the {@link ModuleType} of the check which {@link ViolationLevelManagement} is searched for.
     *
     * @return the check if it was found
     *
     * @throws IllegalArgumentException            if the provided {@link ModuleType} parameter is not used in a check
     * @throws NoViolationLevelManagementException if the module doesn't have a {@link ViolationLevelManagement}
     */
    public ViolationLevelManagement getViolationLevelManagement(final ModuleType moduleType)
    {
        final Module module = this.getModule(moduleType);
        if (module instanceof ViolationModule)
        {
            return ((ViolationModule) module).getViolationLevelManagement();
        }

        throw new NoViolationLevelManagementException(moduleType);
    }

    /**
     * Enables or disables a check in runtime
     *
     * @param moduleType the {@link ModuleType} of the check that should be disabled.
     */
    public void setStateOfModule(final ModuleType moduleType, final boolean state)
    {
        final Module module = this.getModule(moduleType);

        // The message that will be printed in the logs / console
        // 23 is capacity needed by the message
        // 29 the maximum length of a module.
        // Thus a capacity of 52
        final StringBuilder stringBuilder = new StringBuilder(52);
        stringBuilder.append("Check ");
        stringBuilder.append(module.getName());
        stringBuilder.append(" has been ");

        // Should it be enabled or disabled
        if (state)
        {
            module.enable();
            stringBuilder.append("enabled.");
        }
        else
        {
            module.disable();
            stringBuilder.append("disabled.");
        }

        // Send / log the message
        VerboseSender.sendVerboseMessage(stringBuilder.toString());
    }
}