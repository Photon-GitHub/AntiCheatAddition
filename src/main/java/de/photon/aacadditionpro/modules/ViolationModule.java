package de.photon.aacadditionpro.modules;

import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import me.konsolas.aac.api.AACCustomFeature;

import java.util.Collections;
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

    default AACCustomFeature getAACFeature(UUID uuid)
    {
        return new AACCustomFeature(this.getConfigString(), this.getModuleType().getInfo(), this.getViolationLevelManagement().getVL(uuid), getAACTooltip(uuid));
    }

    default Map<String, String> getAACTooltip(UUID uuid)
    {
        return Collections.emptyMap();
    }
}
