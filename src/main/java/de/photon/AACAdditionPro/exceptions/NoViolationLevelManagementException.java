package de.photon.AACAdditionPro.exceptions;

import de.photon.AACAdditionPro.AdditionHackType;

public class NoViolationLevelManagementException extends RuntimeException
{
    private final AdditionHackType additionHackType;

    public NoViolationLevelManagementException(final AdditionHackType additionHackType)
    {
        super("The check of the " + additionHackType.name() + " has no ViolationLevels.");
        this.additionHackType = additionHackType;
    }

    public AdditionHackType getAdditionHackType()
    {
        return additionHackType;
    }
}
