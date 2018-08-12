package de.photon.AACAdditionPro.checks;

import de.photon.AACAdditionPro.Module;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;

/**
 * This interface defines a {@link Module} which has a {@link ViolationLevelManagement}.
 */
public interface ViolationModule extends Module
{
    /**
     * @return the {@link ViolationLevelManagement} of the check.
     */
    ViolationLevelManagement getViolationLevelManagement();
}
