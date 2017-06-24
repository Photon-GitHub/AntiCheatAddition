package de.photon.AACAdditionPro.checks;

import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.Module;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;

public interface AACAdditionProCheck extends Module
{
    default String getName()
    {
        return this.getAdditionHackType().getConfigString();
    }

    AdditionHackType getAdditionHackType();

    @Override
    default void subEnable() {}


    @Override
    default void subDisable() {}

    /**
     * @return the {@link ViolationLevelManagement} of the check.<br>
     * By default the check has no {@link ViolationLevelManagement} and this {@link java.lang.reflect.Method} returns null.
     */
    default ViolationLevelManagement getViolationLevelManagement()
    {
        return null;
    }

    /**
     * Used to see if a {@link AACAdditionProCheck} has a {@link ViolationLevelManagement}.
     *
     * @return true if the {@link AACAdditionProCheck} has a {@link ViolationLevelManagement} and false if not.
     */
    default boolean hasViolationLevelManagement()
    {
        return this.getViolationLevelManagement() != null;
    }
}
