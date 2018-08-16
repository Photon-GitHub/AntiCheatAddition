package de.photon.AACAdditionPro.modules;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.exceptions.NoViolationLevelManagementException;
import de.photon.AACAdditionPro.util.files.configs.Configs;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Set;

/**
 * Manages the {@link Module}s of {@link AACAdditionPro}.
 * It extends {@link ArrayList} and thus has an in-built iterator.
 */
public final class ModuleManager
{
    @Getter
    private final Set<Module> modules;

    public ModuleManager(final Set<Module> modules)
    {
        this.modules = modules;
        this.modules.forEach(Module::enableModule);

        // Invoke the changing of configs after all enable calls.
        Configs.saveChangesForAllConfigs();
    }

    /**
     * Tries to orderly disable all modules.
     */
    public void shutdown()
    {
        // Disable all checks
        try
        {
            this.modules.forEach(Module::disableModule);
        } catch (NullPointerException ignore)
        {
            // This can happen if the modulemanager is already finalized.
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
        for (final Module module : this.modules)
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
        if (state)
            Module.enableModule(this.getModule(moduleType));
        else
            Module.disableModule(this.getModule(moduleType));
    }
}