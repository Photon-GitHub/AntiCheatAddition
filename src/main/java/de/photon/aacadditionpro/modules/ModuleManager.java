package de.photon.aacadditionpro.modules;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.util.files.configs.Configs;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import lombok.Getter;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Manages the {@link Module}s of {@link AACAdditionPro}.
 */
public final class ModuleManager
{
    @Getter
    private final Map<ModuleType, Module> modules = new EnumMap<>(ModuleType.class);

    public ModuleManager(final Set<Module> modules)
    {
        for (Module module : modules) {
            this.modules.put(module.getModuleType(), module);
            Module.enableModule(module);
        }

        // Invoke the changing of configs after all enable calls.
        Configs.saveChangesForAllConfigs();
    }

    /**
     * Gets a {@link Module} from its {@link ModuleType}.
     *
     * @param moduleType the {@link ModuleType} of the {@link Module} that should be found
     *
     * @return the {@link Module} if a match was found
     */
    public Module getModule(final ModuleType moduleType)
    {
        return this.modules.get(moduleType);
    }

    /**
     * Gets a the {@link ViolationLevelManagement} of a module from its {@link ModuleType}.
     *
     * @param moduleType the {@link ModuleType} of the check which {@link ViolationLevelManagement} is searched for.
     *
     * @return the check if it was found
     *
     * @throws IllegalArgumentException if the check does not have a {@link ViolationLevelManagement}.
     */
    public ViolationLevelManagement getViolationLevelManagement(final ModuleType moduleType)
    {
        final Module module = this.getModule(moduleType);
        Preconditions.checkArgument(module instanceof ViolationModule, "ModuleType " + moduleType.name() + " does not reference to a module with a ViolationLevelManagement.");
        return ((ViolationModule) module).getViolationLevelManagement();
    }

    /**
     * Checks if a {@link Module} of a certain {@link ModuleType} is enabled.
     *
     * @param moduleType the {@link ModuleType} of the module that should be checked
     *
     * @return <code> true</code> if the module referred to by the {@link ModuleType} is enabled, else <code>false</code>
     */
    public boolean getStateOfModule(final ModuleType moduleType)
    {
        return moduleType.isEnabled();
    }

    /**
     * Enables or disables a check in runtime
     *
     * @param moduleType the {@link ModuleType} of the module that should be enabled or disabled.
     */
    public void setStateOfModule(final ModuleType moduleType, final boolean state)
    {
        // This will prevent bugs with registering modules multiple times
        if (state != getStateOfModule(moduleType)) {
            if (state) {
                Module.enableModule(this.getModule(moduleType));
            } else {
                Module.disableModule(this.getModule(moduleType));
            }
        }
    }
}