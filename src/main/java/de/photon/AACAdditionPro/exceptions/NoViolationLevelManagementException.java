package de.photon.AACAdditionPro.exceptions;

import de.photon.AACAdditionPro.modules.ModuleType;
import lombok.Getter;

public class NoViolationLevelManagementException extends RuntimeException
{
    @Getter
    private final ModuleType moduleType;

    public NoViolationLevelManagementException(final ModuleType moduleType)
    {
        super("The module of the " + moduleType.name() + " has no ViolationLevelManagement.");
        this.moduleType = moduleType;
    }
}
