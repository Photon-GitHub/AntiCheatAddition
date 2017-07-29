package de.photon.AACAdditionPro.exceptions;

import de.photon.AACAdditionPro.AdditionHackType;

public class NoViolationLevelManagementExeption extends RuntimeException
{
    private final AdditionHackType additionHackType;

    public NoViolationLevelManagementExeption(final AdditionHackType additionHackType)
    {
        super("The check of the " + additionHackType.name() + " has no ViolationLevels.");
        this.additionHackType = additionHackType;
    }

    public AdditionHackType getAdditionHackType()
    {
        return additionHackType;
    }
}
