package de.photon.aacadditionpro.modules;

import com.google.common.collect.ImmutableMap;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;

import java.util.Map;
import java.util.UUID;

/**
 * This interface defines a {@link Module} which has a {@link ViolationLevelManagement}.
 */
public interface ViolationModule extends Module
{
    /**
     * @return the {@link ViolationLevelManagement} of the check.
     */
    ViolationLevelManagement getViolationLevelManagement();

    default Map<String, String> getAACTooltip(UUID uuid, double score)
    {
        return ImmutableMap.of("Score:", Double.toString(score));
    }
}
