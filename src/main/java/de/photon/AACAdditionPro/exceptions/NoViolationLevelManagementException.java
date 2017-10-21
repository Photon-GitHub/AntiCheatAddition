package de.photon.AACAdditionPro.exceptions;

import de.photon.AACAdditionPro.ModuleType;

public class NoViolationLevelManagementException extends RuntimeException
{
    private final ModuleType moduleType;

    public NoViolationLevelManagementException(final ModuleType moduleType)
    {
        super("The module of the " + moduleType.name() + " has no ViolationLevelManagement.");
        this.moduleType = moduleType;
    }

    public ModuleType getModuleType()
    {
        return moduleType;
    }
}
