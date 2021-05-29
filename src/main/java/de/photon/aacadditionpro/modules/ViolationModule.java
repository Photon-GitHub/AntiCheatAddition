package de.photon.aacadditionpro.modules;

import com.google.common.collect.ImmutableMap;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.util.violationlevels.ViolationAggregateManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import de.photon.aacadditionpro.util.violationlevels.threshold.ThresholdManagement;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class ViolationModule extends Module
{
    @Getter(lazy = true) private final ViolationManagement management = createViolationManagement();
    @Getter private final String aacInfo;
    private final double aacScoreMultiplier;

    public ViolationModule(String configString)
    {
        super(configString);
        this.aacInfo = AACAdditionPro.getInstance().getConfig().getString(configString + ".aac_status_message", null);
        this.aacScoreMultiplier = AACAdditionPro.getInstance().getConfig().getDouble(configString + ".aac_score_multiplier", 1.0);
    }

    public static ViolationModule parentOf(String configString, ViolationModule... children)
    {
        return new ViolationModule(configString)
        {
            @Override
            protected ViolationManagement createViolationManagement()
            {
                return new ViolationAggregateManagement(this, ThresholdManagement.loadThresholds(this.getConfigString()), Arrays.stream(children).map(violationModule -> getManagement()).collect(Collectors.toSet()));
            }

            @Override
            protected ModuleLoader createModuleLoader()
            {
                return ModuleLoader.builder(this).build();
            }
        };
    }

    public double getAACScore(UUID uuid)
    {
        return this.management.getVL(uuid) * aacScoreMultiplier;
    }

    public Map<String, String> getAACTooltip(UUID uuid, double score)
    {
        return ImmutableMap.of("Score:", Double.toString(score));
    }

    protected abstract ViolationManagement createViolationManagement();
}
