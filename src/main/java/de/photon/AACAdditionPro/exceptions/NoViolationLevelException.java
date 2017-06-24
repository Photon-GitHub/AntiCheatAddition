package de.photon.AACAdditionPro.exceptions;

import de.photon.AACAdditionPro.AdditionHackType;

public class NoViolationLevelException extends RuntimeException
{
    private final AdditionHackType additionHackType;

    public NoViolationLevelException(final AdditionHackType additionHackType)
    {
        super("The check of the additionHackType has no ViolationLevels.");
        this.additionHackType = additionHackType;
    }

    public AdditionHackType getAdditionHackType()
    {
        return additionHackType;
    }
}
