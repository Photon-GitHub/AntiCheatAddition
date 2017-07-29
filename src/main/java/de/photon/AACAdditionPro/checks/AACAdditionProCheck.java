package de.photon.AACAdditionPro.checks;

import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.Module;
import de.photon.AACAdditionPro.exceptions.NoViolationLevelManagementExeption;
import de.photon.AACAdditionPro.userdata.User;
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
     * By default the check has no {@link ViolationLevelManagement} and this {@link java.lang.reflect.Method} throws a {@link NoViolationLevelManagementExeption}.
     */
    default ViolationLevelManagement getViolationLevelManagement() throws NoViolationLevelManagementExeption
    {
        throw new NoViolationLevelManagementExeption(this.getAdditionHackType());
    }

    @Override
    default String getConfigString()
    {
        return this.getAdditionHackType().getConfigString();
    }

    /**
     * @return true if the {@link de.photon.AACAdditionPro.userdata.User} is null or bypassed.
     */
    static boolean isUserInvalid(User user)
    {
        return user == null || user.isBypassed();
    }
}
