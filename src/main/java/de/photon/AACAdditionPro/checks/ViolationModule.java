package de.photon.AACAdditionPro.checks;

import de.photon.AACAdditionPro.Module;
import de.photon.AACAdditionPro.exceptions.NoViolationLevelManagementException;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;

public interface ViolationModule extends Module
{
    /**
     * @return the {@link ViolationLevelManagement} of the check.<br>
     * By default the check has no {@link ViolationLevelManagement} and this {@link java.lang.reflect.Method} throws a {@link NoViolationLevelManagementException}.
     */
    default ViolationLevelManagement getViolationLevelManagement() throws NoViolationLevelManagementException
    {
        throw new NoViolationLevelManagementException(this.getModuleType());
    }
}
